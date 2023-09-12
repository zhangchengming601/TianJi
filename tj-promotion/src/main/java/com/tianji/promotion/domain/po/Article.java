package com.tianji.promotion.domain.po;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "article") //指定要对应的文档名（表名）
@Accessors(chain = true)
public class Article {
    @Id
    private String id;//文章主键

    private String articleName; //文章名

    private String content; //文章内容
}

