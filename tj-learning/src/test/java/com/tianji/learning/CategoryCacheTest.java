package com.tianji.learning;


import com.tianji.api.cache.CategoryCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CategoryCacheTest {
    @Autowired
    private CategoryCache categoryCache;

    @Test
    public void test(){
        String categoryNames = categoryCache.getCategoryNames(List.of(1001l, 2001l, 3003l));
        System.out.println(categoryNames);
    }

}
