package com.mall2.common;

import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor

public class CommonPage<T> {
    private Integer pageNum;      // 当前页码
    private Integer pageSize;     // 每页条数
    private Long total;           // 总记录数
    private Integer totalPage;    // 总页数
    private List<T> list;         // 数据列表

    /**
     * 将 PageHelper 的 PageInfo 转换为 CommonPage
     * @param list PageHelper 返回的 List（实际是 Page 对象）
     */
    public static <T> CommonPage<T> restPage(List<T> list) {
        CommonPage<T> result = new CommonPage<>();
        PageInfo<T> pageInfo = new PageInfo<>(list);
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());
        result.setTotal(pageInfo.getTotal());
        result.setTotalPage(pageInfo.getPages());
        result.setList(pageInfo.getList());
        return result;
    }
}
