package com.tianji.learning.serviceTest;

import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.service.IPointsBoardService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
@RequiredArgsConstructor
public class PointsBoardTest {

    @Autowired
    IPointsBoardService pointsBoardService;


    @Test
    public void test01(){
        PointsBoard board = new PointsBoard();
        board.setId(1l);
        board.setUserId(2l);
        board.setPoints(99);
        ArrayList<PointsBoard> pointsBoards = new ArrayList<>();
        pointsBoards.add(board);

        pointsBoardService.savePointsBoardList(pointsBoards);
    }
}
