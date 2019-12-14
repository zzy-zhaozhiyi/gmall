package com.atguigu.gmall.search.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.search.vo.SearchParam;
import com.atguigu.gmall.search.vo.SearchResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zzy
 * @create 2019-12-10 23:16
 */
@RestController
@RequestMapping("search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @PostMapping
    public Resp<SearchResponseVO> search(SearchParam searchParam) throws Exception {

        SearchResponseVO responseVO = this.searchService.search(searchParam);
        return Resp.ok(responseVO);
    }
}