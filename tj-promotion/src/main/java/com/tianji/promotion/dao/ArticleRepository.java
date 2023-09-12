package com.tianji.promotion.dao;


import com.tianji.promotion.domain.po.Article;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ArticleRepository extends MongoRepository<Article,String> {
    //根据id查询文章
    List<Article> findByid(String id);
}
