package com.mall2.Product.controller;

import com.mall2.Product.dto.ProductQueryParam;
import com.mall2.Product.model.Product;
import com.mall2.Product.service.ProductService;
import com.mall2.common.CommonPage;
import com.mall2.common.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 分页查询商品列表
     * 校验：pageNum 和 pageSize 由 @Valid 触发 ProductQueryParam 中的校验
     */
    @GetMapping("/list")
    public CommonResult<CommonPage<Product>> list(@Valid ProductQueryParam param) {
        List<Product> productList = productService.list(param);
        return CommonResult.success(CommonPage.restPage(productList));
    }

    @GetMapping("/detail/{id}")
    public CommonResult<Product> detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        return CommonResult.success(product);
    }

    /**
     * 新增商品
     * 校验：触发 Product 实体类中的字段校验
     */
    @PostMapping("/create")
    public CommonResult<Void> create(@Valid @RequestBody Product product) {
        productService.create(product);
        return CommonResult.success(null, "新增成功");
    }

    /**
     * 更新商品
     * 校验：只要求 ID 不为空（Product 实体类中 id 已加 @NotNull）
     */
    @PostMapping("/update")
    public CommonResult<Void> update(@Valid @RequestBody Product product) {
        productService.update(product);
        return CommonResult.success(null, "更新成功");
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return CommonResult.success(null, "删除成功");
    }

    // ==================== 业务操作接口 ====================

    @PostMapping("/publish/{id}")
    public CommonResult<Void> publish(@PathVariable Long id) {
        productService.publish(id);
        return CommonResult.success(null, "上架成功");
    }

    @PostMapping("/unpublish/{id}")
    public CommonResult<Void> unpublish(@PathVariable Long id) {
        productService.unpublish(id);
        return CommonResult.success(null, "下架成功");
    }

    @PostMapping("/verify/pass/{id}")
    public CommonResult<Void> verifyPass(@PathVariable Long id) {
        productService.verifyPass(id);
        return CommonResult.success(null, "审核通过");
    }

    @PostMapping("/verify/reject/{id}")
    public CommonResult<Void> verifyReject(@PathVariable Long id,
                                           @RequestParam(required = false) String reason) {
        productService.verifyReject(id, reason);
        return CommonResult.success(null, "审核驳回");
    }
}