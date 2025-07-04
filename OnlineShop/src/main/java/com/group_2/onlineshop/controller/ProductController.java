package com.group_2.onlineshop.controller;

import com.group_2.onlineshop.dto.ImageDTO;
import com.group_2.onlineshop.dto.ProductDTO;
import com.group_2.onlineshop.entity.Image;
import com.group_2.onlineshop.entity.Product;
import com.group_2.onlineshop.repository.ProductRepository;
import com.group_2.onlineshop.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Lấy danh sách sản phẩm với phân trang: http://localhost:8080/api/products?page=0&size=10&sort=id,asc
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        // Phân tách trường sắp xếp và hướng sắp xếp
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sortParams[1]);
        Sort sortBy = Sort.by(sortDirection, sortField);

        // Tạo Pageable với trang, kích thước, và sắp xếp
        Pageable pageable = PageRequest.of(page, size, sortBy);

        // Lấy danh sách sản phẩm phân trang
        Page<Product> productPage = productRepository.findAll(pageable);

        // Chuyển đổi sang Page<ProductDTO>
        Page<ProductDTO> productDTOPage = productPage.map(this::convertToDTO);

        return ResponseEntity.ok(productDTOPage);
    }

    // Lấy thông tin chi tiết sản phẩm bằng Id: http://localhost:8080/api/products/1
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tạo một sản phẩm mới (chỉ ADMIN có quyền): http://localhost:8080/api/products
    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Product product
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        Product savedProduct = productRepository.save(product);
        return ResponseEntity.ok(convertToDTO(savedProduct));
    }

    // Cập nhập sản phẩm (chỉ ADMIN có quyền): http://localhost:8080/api/products/4
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            updatedProduct.setId(id);
            Product savedProduct = productRepository.save(updatedProduct);
            return ResponseEntity.ok(convertToDTO(savedProduct));
        }
        return ResponseEntity.notFound().build();
    }

    // Lấy danh sách sản phẩm theo danh mục: http://localhost:8080/api/products?categoryId={id}
    @GetMapping(params = "categoryId")
    public ResponseEntity<Page<ProductDTO>> getProductsByCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        // Phân tích tham số sort
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sortParams[1]);
        Sort sortBy = Sort.by(sortDirection, sortField);

        // Tạo Pageable
        Pageable pageable = PageRequest.of(page, size, sortBy);

        // Kiểm tra categoryId tồn tại (tùy chọn)
        if (categoryId == null) {
            return ResponseEntity.badRequest().build();
        }

        // Lấy dữ liệu phân trang
        Page<Product> productPage = productRepository.findByCategoryId(categoryId, pageable);
        Page<ProductDTO> productDTOPage = productPage.map(this::convertToDTO);

        return ResponseEntity.ok(productDTOPage);
    }

    // Xoá 1 sản phẩm (chỉ ADMIN có quyền): http://localhost:8080/api/products/4
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid Authorization header");
        }

        String token = authorizationHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        if (!jwtUtil.validateToken(token, username)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Search and filter products with pagination and sorting
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchAndFilterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sortParams[1]);
        Sort sortBy = Sort.by(sortDirection, sortField);

        Pageable pageable = PageRequest.of(page, size, sortBy);

        Page<Product> productPage = productRepository.searchAndFilterProducts(keyword, categoryId, minPrice, maxPrice, inStock, pageable);

        Page<ProductDTO> productDTOPage = productPage.map(this::convertToDTO);
        return ResponseEntity.ok(productDTOPage);
    }

    @GetMapping("/search-by-keyword")
    public ResponseEntity<Page<ProductDTO>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(getProducts(page, size, sort));
        }

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sortParams[1]);
        Sort sortBy = Sort.by(sortDirection, sortField);

        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword, pageable);
        Page<ProductDTO> productDTOPage = productPage.map(this::convertToDTO);

        return ResponseEntity.ok(productDTOPage);
    }

    public Page<ProductDTO> getProducts(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection = Sort.Direction.fromString(sortParams[1]);
        Sort sortBy = Sort.by(sortDirection, sortField);

        Pageable pageable = PageRequest.of(page, size, sortBy);
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(this::convertToDTO);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(product.getId());
        productDTO.setName(product.getName());
        productDTO.setPrice(product.getPrice());
        productDTO.setSalePrice(product.getSalePrice());
        productDTO.setDescription(product.getDescription());
        productDTO.setStock(product.getStock());
        productDTO.setBrand(product.getBrand());
        productDTO.setOrigin(product.getOrigin());
        productDTO.setSoldQuantity(product.getSoldQuantity());
        productDTO.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        productDTO.setImages(product.getImages() != null ?
                product.getImages().stream().map(this::convertToImageDTO).collect(Collectors.toList()) : null);
        return productDTO;
    }

    private ImageDTO convertToImageDTO(Image image) {
        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setId(image.getId());
        imageDTO.setUrl(image.getUrl());
        imageDTO.setPublicId(image.getPublicId());
        return imageDTO;
    }
}