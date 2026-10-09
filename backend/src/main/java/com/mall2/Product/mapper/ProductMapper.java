package com.mall2.Product.mapper;

import com.mall2.Product.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface ProductMapper {
    /**
     * 条件分页查询商品列表
     */
    List<Product> selectByQuery(@Param("keyword") String keyword,
                                @Param("productSn") String productSn,
                                @Param("categoryId") Long categoryId,
                                @Param("brandId") Long brandId,
                                @Param("publishStatus") Integer publishStatus,
                                @Param("verifyStatus") Integer verifyStatus);

    /**
     * 根据ID查询商品
     */
    Product selectById(@Param("id") Long id);

    /**
     * 新增商品
     */
    void insert(Product product);

    /**
     * 更新商品
     */
    void updateById(Product product);

    /**
     * 逻辑删除商品
     */
    void deleteById(@Param("id") Long id);

    /**
     * 乐观锁扣减库存
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @param version   乐观锁版本号
     * @return 影响行数（0表示扣减失败，被别人抢先了）
     */
    int decreaseStock(@Param("productId") Long productId,
                      @Param("quantity") Integer quantity,
                      @Param("version") Integer version);

//撤销订单后要把商品的库存重新加回去
    /**
     * 乐观锁：增加库存（订单取消回补库存）
     * @param productId 商品id
     * @param quantity 回补数量
     * @param version 当前版本号
     */
    int increaseStock(@Param("productId") Long productId,
                      @Param("quantity") Integer quantity,
                      @Param("version") Integer version);
}
