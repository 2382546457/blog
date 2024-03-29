package com.xiaohe.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaohe.domain.entity.Category;
import com.xiaohe.domain.entity.PageVo;
import com.xiaohe.domain.vo.UpdateCategoryVo;
import com.xiaohe.mapper.CategoryMapper;
import com.xiaohe.service.CategoryService;
import io.jsonwebtoken.lang.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-01-03 13:29
 */
@Service
@Transactional
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getCategoryList() {
        return categoryMapper.getCategoryList();
    }

    /**
     * 分页查询
     * @param pageNum 现在的页码
     * @param pageSize 每一页的数量
     * @param name 分类名
     * @param status 分类的状态
     * @return
     */
    @Override
    public PageVo selectPageList(Integer pageNum, Integer pageSize, String name, String status) {
        LambdaQueryWrapper<Category> lambda = new LambdaQueryWrapper<>();
        lambda.like(Strings.hasText(name),Category::getName, name);
        lambda.eq(Strings.hasText(status), Category::getStatus, status);
        Page<Category> page = new Page<>();
        page.setSize(pageSize);
        page.setCurrent(pageNum);
        Page<Category> page1 = page(page, lambda);

        PageVo pageVo = new PageVo();
        pageVo.setRows(page1.getRecords());
        pageVo.setTotal(page1.getTotal());

        return pageVo;
    }

    /**
     * 新增分类
     * @param category
     * @return
     */
    @Override
    @Transactional
    public boolean addCategory(Category category) {
        // 如果分类名重复，不能添加
        LambdaQueryWrapper<Category> lambda = new LambdaQueryWrapper<>();
        lambda.eq(Category::getName, category.getName());
        Category category1 = categoryMapper.selectOne(lambda);
        if (!Objects.isNull(category1)) {
            throw new RuntimeException("分类名重复!");
        }
        // 开始添加
        return save(category);
    }

    /**
     * 批量删除分类
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public boolean daleteCategorys(List<Long> ids) {
        // 查询分类下是否有文章，有则不能删除
        List<Category> categories = categoryMapper.selectBatchIds(ids);
        if (Objects.isNull(categories) || categories.size() == 0) {
            throw new RuntimeException("该分类下有文章，不能删除");
        }
        int i = categoryMapper.deleteBatchIds(ids);
        return i == ids.size();
    }

    /**
     * 更改分类
     * @param category
     * @return
     */
    @Override
    public boolean updateCategory(UpdateCategoryVo category) {
        // 直接updte
        Category category1 = new Category();
        BeanUtils.copyProperties(category, category1);

        return updateById(category1);
    }
}
