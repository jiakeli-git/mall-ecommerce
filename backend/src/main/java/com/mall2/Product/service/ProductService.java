package com.mall2.Product.service;

import com.mall2.Product.dto.ProductQueryParam;
import com.mall2.Product.model.Product;

import java.util.List;

public interface ProductService {
    /**
     * 条件分页查询商品列表
     */
    List<Product> list(ProductQueryParam param);

    /**
     * 根据ID查询商品
     */
    Product getById(Long id);

    /**
     * 新增商品
     */
    void create(Product product);

    /**
     * 更新商品
     */
    void update(Product product);

    /**
     * 逻辑删除商品
     */
    void delete(Long id);
    // ==================== 业务操作 ====================

    /**
     * 上架商品（仅允许已审核通过、未删除的商品）
     */
    void publish(Long id);

    /**
     * 下架商品（仅允许已上架的商品）
     */
    void unpublish(Long id);

    /**
     * 审核通过（仅允许待审核的商品）
     */
    void verifyPass(Long id);

    /**
     * 审核驳回（仅允许待审核的商品，驳回后可重新编辑提交）
     */
    void verifyReject(Long id, String rejectReason);

}
