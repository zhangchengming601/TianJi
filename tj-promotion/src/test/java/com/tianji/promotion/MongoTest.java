package com.tianji.promotion;


import com.tianji.promotion.domain.po.Article;
import com.tianji.promotion.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MongoTest {

    @Autowired
    private  ArticleServiceImpl articleService;


    @Test
    public void test1(){
        Article article = new Article();
        article.setId("1");
        article.setArticleName("tijian");
        article.setContent("1111111111");

        articleService.create(article);
    }
}
