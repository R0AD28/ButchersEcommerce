package com.ecommerce.catalog_service.controller;
import com.ecommerce.catalog_service.dto.*;
import com.ecommerce.catalog_service.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController 
@RequestMapping("/products") @Validated
public class ProductController {
        private final ProductService service;
        
        public ProductController(ProductService service){this.service=service;}
        
        @GetMapping 
        @PreAuthorize("hasAuthority('VIEW_PRODUCTS')")
        public ResponseEntity<PageResponse<ProductResponse>> 
                findPublic(@RequestParam(defaultValue="0") 
                        int page,@RequestParam(defaultValue="20") 
                        int size
                        ){
                                return ResponseEntity.ok(service.findPublic(page,size)
                                );
                        }
        
        @GetMapping("/detail") 
        @PreAuthorize("hasAuthority('VIEW_PRODUCTS')")
        public ResponseEntity<ProductResponse> 
        detail(
                @RequestParam @NotBlank String sku
                ){
                        return ResponseEntity.ok(service.findPublicBySku(sku)
                        );
                }
        
        @GetMapping("/manage") 
        @PreAuthorize("hasAnyAuthority('CREATE_PRODUCT','UPDATE_PRODUCT','DELETE_PRODUCT')")
        public ResponseEntity<PageResponse<ManagementProductResponse>> 
                manage(@RequestParam(defaultValue="false")
                        boolean includeInactive,@RequestParam(defaultValue="0") 
                        int page,@RequestParam(defaultValue="20") 
                        int size
                        )
                {
                        return ResponseEntity.ok(service.findForManagement(includeInactive,page,size)
                        );
                }
        
        @PostMapping 
        @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
        public ResponseEntity<ManagementProductResponse> 
                create(
                        @Valid @RequestBody CreateProductRequest request,Authentication auth,HttpServletRequest http
                        )
                {
                        ManagementProductResponse 
                        r=service.create(request,userId(auth),email(auth),http.getRemoteAddr()
                        );
                                return ResponseEntity.created(URI.create("/products/detail?sku="+r.sku())
                        ).body(r);
                }
        
        @PutMapping 
        @PreAuthorize("hasAuthority('UPDATE_PRODUCT')")
        public ResponseEntity<ManagementProductResponse> 
                update(@Valid @RequestBody UpdateProductRequest request,Authentication auth,HttpServletRequest http
                ){
                        return ResponseEntity.ok(service.update(request,userId(auth),email(auth),http.getRemoteAddr())
                );
                }
        
        @PatchMapping("/inventory")
        @PreAuthorize("hasAuthority('UPDATE_PRODUCT')")
        public ResponseEntity<ManagementProductResponse> 
                inventory(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody InventoryAdjustmentRequest request,Authentication auth,HttpServletRequest http
                ){
                        return ResponseEntity.ok(service.adjustInventory(request,key,userId(auth),email(auth),http.getRemoteAddr())
                );
                }
        
        @PatchMapping("/status") 
        @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
        public ResponseEntity<ManagementProductResponse> 
                status(
                        @Valid @RequestBody ProductStatusRequest request,Authentication auth,HttpServletRequest http
                        ){
                                return ResponseEntity.ok(service.changeStatus(request,userId(auth),email(auth),http.getRemoteAddr())
                                );
                        }
        private Long userId(Authentication a)
        {
                return (Long)a.getPrincipal();
        } 
        
        private String email(Authentication a)
        {
                return (String)a.getDetails();
        }
}
