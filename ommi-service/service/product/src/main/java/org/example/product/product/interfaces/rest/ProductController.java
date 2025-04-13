package org.example.product.product.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.product.ProductRequest;
import org.example.product.product.application.model.product.ProductResponse;
import org.example.product.product.application.model.product.ProductVariantRequest;
import org.example.product.product.application.service.product.ProductWriteService;
import org.example.product.product.domain.product.model.ProductId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductWriteService productWriteService;

    public record Response(String message) {
    }

    @GetMapping("/test")
    public Response test() {
        return new Response("product:test");
    }

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody @Valid ProductRequest request) throws ExecutionException, InterruptedException, IOException {
        int storeId = 1;
        var productId = productWriteService.create(storeId, request);
        return null;
    }

    @PostMapping(value = "/{id}")
    public ProductResponse updateProduct(@PathVariable int id,
                                         @RequestBody @Valid ProductRequest request
    ) throws IOException, ExecutionException, InterruptedException {
        int storeId = 1;
        var productId = new ProductId(storeId, id);
        productWriteService.updateProduct(productId, request);
        return null;
    }

    @PostMapping("/{product_id}/variants")
    public void createProductVariant(@PathVariable("product_id") int productId,
                                     @RequestBody @Valid ProductVariantRequest request
    ) {
        int storeId = 1;
        var variantId = productWriteService.createVariant(new ProductId(storeId, productId), request);
    }
}
