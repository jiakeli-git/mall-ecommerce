package com.mall2.Product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Update;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    /**
            * 更新时必须传 ID，新增时 ID 为自增，不需要传
     */
    @NotNull(message = "商品ID不能为空", groups = Update.class)
    private Long id;//索引
    /**
     * 货号，由后端生成，前端可不传
     */
    private String productSn;//货号
    /**
     * 商品名称，新增时必填
     */
    @NotBlank(message = "商品名称不能为空")
    private String name;//产品名称
    private String subTitle;//副标题/卖点
    private Long categoryId;//商品所属种类
    private Long brandId;//商品所属品牌
    /**
     * 销售价格，新增时必填
     */
    @NotNull(message = "销售价格不能为空")
    private BigDecimal price;//商品销售价格
    private BigDecimal originalPrice;//商品进价原价
    /**
     * 库存，新增时必填
     */
    @NotNull(message = "库存不能为空")
    private Integer stock;//商品库存
    private String unit;//计量单位（如件，个）
    private String pic;//商品主图url
    private String albumPics;//商品轮播图
    private String description;//商品简介描述
    private String detailHtml;//商品富文本详情（图片加文字）
    private Integer sale;//商品销量
    private Integer sort;//商品排序数字参考

    private Integer publishStatus;   // 0-下架 1-上架
    private Integer verifyStatus;    // 0-待审核 1-通过 2-驳回
    private String rejectReason;     // 审核驳回时的具体原因，运营/供应商可见
    private Integer deleteStatus;    // 0-未删除 1-已删除

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String createBy;//数据操作人员
    /** 乐观锁版本号 */
    private Integer version;
    /**
     * 分组接口：用于区分新增和更新场景
     * 更新时只校验 ID，新增时不校验 ID（自增）
     */
    public interface Update {}
}
