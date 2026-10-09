package com.mall2.Product.service.impl;

import com.github.pagehelper.PageHelper;
import com.mall2.common.Exceptions.BusinessException;
import com.mall2.Product.dto.ProductQueryParam;
import com.mall2.Product.mapper.ProductMapper;
import com.mall2.Product.model.Product;
import com.mall2.Product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Override
    public List<Product> list(ProductQueryParam param) {
        // 启动 PageHelper 分页
        PageHelper.startPage(param.getPageNum(), param.getPageSize());
        // 执行查询
        return productMapper.selectByQuery(
                param.getKeyword(),
                param.getProductSn(),
                param.getCategoryId(),
                param.getBrandId(),
                param.getPublishStatus(),
                param.getVerifyStatus()
        );
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Product product) {
        // 如果货号为空，自动生成
        if (!StringUtils.hasText(product.getProductSn())) {
            product.setProductSn(generateProductSn());
        }
        // 默认状态
        if (product.getPublishStatus() == null) {
            product.setPublishStatus(0);
        }
        if (product.getVerifyStatus() == null) {
            product.setVerifyStatus(0);
        }
        // 设置创建人（实际项目中从 SecurityContext 获取当前用户）
        product.setCreateBy("admin");
        productMapper.insert(product);
    }

    @Override
    public void update(Product product) {
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long id) {
        productMapper.deleteById(id);
    }

    // ==================== 业务操作方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Product product = getById(id);
        // 新增判空，优先校验商品是否存在
        if (product == null) {
            throw new BusinessException("该商品不存在");
        }
        // 业务校验：仅允许审核通过且未删除的商品上架
        if (product.getVerifyStatus() != 1) {
            throw new BusinessException("商品尚未审核通过，无法上架");
        }
        if (product.getPublishStatus() == 1) {
            throw new BusinessException("商品已处于上架状态，请勿重复操作");
        }
        if(product.getDeleteStatus() == 1){
            throw new BusinessException("该商品已删除，不能上架");
        }

        // 执行业务：更新状态
        Product update = new Product();
        update.setId(id);
        update.setPublishStatus(1);
        productMapper.updateById(update);
        // 可记录操作日志（略）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        Product product = getById(id);
        // 新增判空，优先校验商品是否存在
        if (product == null) {
            throw new BusinessException("该商品不存在");
        }
        if (product.getPublishStatus() == 0) {
            throw new BusinessException("商品已处于下架状态，请勿重复操作");
        }
        Product update = new Product();
        update.setId(id);
        update.setPublishStatus(0);
        productMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyPass(Long id) {
        Product product = getById(id);
        // 新增判空，优先校验商品是否存在
        if (product == null) {
            throw new BusinessException("该商品不存在");
        }
        if (product.getVerifyStatus() != 0) {
            throw new BusinessException("该商品当前状态不允许审核通过（仅待审核状态可操作）");
        }
        Product update = new Product();
        update.setId(id);
        update.setVerifyStatus(1);
        productMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyReject(Long id, String rejectReason) {
        // 1. 业务校验：只有待审核（状态为0）才能驳回
        Product product = getById(id);
        // 新增判空，优先校验商品是否存在
        if (product == null) {
            throw new BusinessException("该商品不存在");
        }
        if (product.getVerifyStatus() != 0) {
            throw new BusinessException("该商品当前状态不允许驳回（仅待审核状态可操作）");
        }

        // 2. 构造更新对象，同时设置状态和驳回原因
        Product update = new Product();
        update.setId(id);
        update.setVerifyStatus(2);          // 状态改为“已驳回”
        update.setRejectReason(rejectReason); // 保存驳回原因（如果传了空字符串或null，XML里的if标签会忽略，但建议给个默认提示）

        // 如果前端没传原因，可以给个默认值，或者抛异常要求必填
        if (!StringUtils.hasText(rejectReason)) {
            throw new BusinessException("驳回原因不能为空，请填写具体原因后重新操作");
        }

        // 3. 执行更新
        productMapper.updateById(update);
    }
    /**
     * 生成货号：P + 年月日 + 4位随机数
     */
    private String generateProductSn() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "P" + date + random;
    }
}
