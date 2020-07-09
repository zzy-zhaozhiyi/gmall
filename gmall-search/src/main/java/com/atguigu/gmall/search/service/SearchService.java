package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.vo.GoodsVO;
import com.atguigu.gmall.search.vo.SearchParam;
import com.atguigu.gmall.search.vo.SearchResponseAttrVO;
import com.atguigu.gmall.search.vo.SearchResponseVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzy
 * @create 2019-12-10 23:23
 */
@Service
public class SearchService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    public SearchResponseVO search(SearchParam searchParam) throws Exception {

        //构建dsl语句，封装成一个方法,返回查询的集合体
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        //根据集合体来查询，相应结果集
        SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //分装方法来解析这个返回的结果
        SearchResponseVO responseVO = this.parseSearchResult(response);
        responseVO.setPageNum(searchParam.getPageNum());
        responseVO.setPageSize(searchParam.getPageSize());
        return responseVO;
    }

    //规定好了vo后，进行设置先把要赋的属性拿出来，设置为null再一一攻克
    private SearchResponseVO parseSearchResult(SearchResponse response) throws JsonProcessingException {
        SearchResponseVO responseVO = new SearchResponseVO();
        //1、查询到的总记录数
        SearchHits hits = response.getHits();
        responseVO.setTotal(hits.totalHits);
        //2、即系品牌的结果集
        SearchResponseAttrVO brand = new SearchResponseAttrVO();//brand类型是这个vo,先new出来，在往里面添值
        brand.setName("品牌");//在页面显示的时候，就是固定的字段
        //2.1查询品牌聚合的结果集，在response中,得到聚合转成map ，这样根据聚合的名称得到每个聚合下面的桶
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");//得到聚合,之所以转化，是为了聚合的实现类，找到桶

        //2.2解析得到的桶，将value转成list
        List<String> brandValues = brandIdAgg.getBuckets().stream().map(bucket -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", bucket.getKeyAsString());//设置id
            Map<String, Aggregation> stringAggregationMap = bucket.getAggregations().asMap();//得到所有子聚合
            ParsedStringTerms brandNameAgg = (ParsedStringTerms) stringAggregationMap.get("brandNameAgg");//根据名称得到具体的聚合，强转到具体的实现类，以便得到下面的桶
            //根据品牌的id下来的肯定只有一个品牌
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name", brandName);
            try {
                return OBJECT_MAPPER.writeValueAsString(map);//将其转化成一个jsonlist来存

            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        brand.setValue(brandValues);
        responseVO.setBrand(brand);
        //3.设置分类的结果
        SearchResponseAttrVO catelog = new SearchResponseAttrVO();//和品牌的类型是一样的
        catelog.setName("分类");
        //设置分类的name
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");//得到具体的聚合
        List<String> categoryValues = categoryIdAgg.getBuckets().stream().map(bucket -> {
            HashMap<String, String> map = new HashMap<>();
            map.put("id", ((Terms.Bucket) bucket).getKeyAsString());//放id的值
            //在分类id下面子聚合找到分类 的名字的名字
            Aggregations aggregations = bucket.getAggregations();
            ParsedStringTerms catogeryNameAgg = (ParsedStringTerms) aggregations.get("catogeryNameAgg");//之所以要强转是因为aggregation是个接口，只有转成实现类，才能找到桶
            String categoryName = catogeryNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name", categoryName);//这个的分类名字是values值中的id和name，而不是外面的
            try {
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;

        }).collect(Collectors.toList());

        responseVO.setCatelog(catelog);
        //4.查询列表解析

        SearchHit[] subHits = hits.getHits();
        List<GoodsVO> goodsList = new ArrayList<>();
        for (SearchHit subHit : subHits) {

            GoodsVO goods = OBJECT_MAPPER.readValue(subHit.getSourceAsString(), new TypeReference<GoodsVO>() {
            });
            //进行高亮的设置
            goods.setTitle(subHit.getHighlightFields().get("title").getFragments()[0].toString());
            goodsList.add(goods);
        }
        responseVO.setProducts(goodsList);

        //5解析属性的操作
        //5.1获取attrAgg的嵌套聚合，并进行强转,强转后获取里面的attrIdAgg的聚合,并进行强转
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<Terms.Bucket> attrIdAggBuckets = (List<Terms.Bucket>) attrIdAgg.getBuckets();//进行强转，下面进行判空,更加严谨
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVO> attrslist = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVO responseAttrVO = new SearchResponseAttrVO();
                //设置属性的值
                responseAttrVO.setProductAttributeId(bucket.getKeyAsNumber().longValue());
                //设置规格参数名,得到桶下面聚合，在强转得到下面的桶，在得到值
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) bucket.getAggregations().get("attrNameAgg");
                String keyAsString = attrNameAgg.getBuckets().get(0).getKeyAsString();
                responseAttrVO.setName(keyAsString);
                //设置规格参数值，和设置参数名同
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) bucket.getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                //这里用到了构造方法的lamda
                List<String> strings = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                responseAttrVO.setValue(strings);
                return responseAttrVO;
            }).collect(Collectors.toList());
            responseVO.setAttrs(attrslist);
        }

        return responseVO;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //根据关键字来进行查询
        String keyword = searchParam.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        //构建查询条件构建器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //1.构建查询条件和过滤条件体,用到了查询工具
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1构建查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        //1.2构建过滤条件
        //1.2.1根据品牌id过滤
        String[] brand = searchParam.getBrand();//得到的是品牌的Id不是品牌，实体类写得不好
        if (brand != null & brand.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brand));
        }
        //1.2.2根据分类的id进行额过滤
        String[] catelog3 = searchParam.getCatelog3();
        if (catelog3 != null & catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }
        //1.2.3根据规格属性嵌套过滤
        String[] props = searchParam.getProps();
        if (props != null & props.length != 0) {
            //可能构建了多个规格的属性嵌套
            for (String prop : props) {
                // 以：进行分割，分割后应该是2个元素，1-attrId  2-attrValue(以-分割的字符串)
                String[] split = StringUtils.split(prop, ":");
                if (split == null || split.length != 2) {
                    continue;
                }
                String[] attrValues = StringUtils.split(split[1], "-");
                //构建嵌套查询
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //嵌套查询中的子查询
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                //构建子查询的过滤条件
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                subBoolQuery.must((QueryBuilders.termsQuery("attrs.attrValue", attrValues)));
                //将过滤条件放进过滤器中
                boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                boolQueryBuilder.filter(boolQuery);
            }
        }
        //1.2.4根据价格区间
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Integer priceFrom = searchParam.getPriceFrom();
        Integer priceTo = searchParam.getPriceTo();
        if (priceFrom != null) {
            rangeQueryBuilder.gte(priceFrom);
        }
        if (priceTo != null) {
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);
        //将整个boolquerybuilder放入searchSourceBuilder进行查询
        searchSourceBuilder.query(boolQueryBuilder);


        //2.构建分页的,在searchparam中，已经进行了初始化了，不用担心异常操作
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        searchSourceBuilder.from((pageNum - 1) * pageSize);
        searchSourceBuilder.size(pageSize);

        //3.进行排序的操作
        String order = searchParam.getOrder();
        String[] split = StringUtils.split(order, ":");
        if (split != null & split.length == 2) {
            String field = null;
            switch (split[0]) {
                case "1":
                    field = "sale";
                    break;
                case "2":
                    field = "price";
                    break;
            }
            searchSourceBuilder.sort(field, StringUtils.equals("asc", split[1]) ? SortOrder.ASC : SortOrder.DESC);
        }

        //4.进行高亮的操作
        searchSourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em>").postTags("</em>"));

        //5.进行品牌的的 聚合的分析
        //5.1根据品牌聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));
        //5.2根据分类来进行聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.3根据嵌套的属性进行聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));


        //6.对于结果集的进行有条件的显示出来，去除无用的字段_source
        searchSourceBuilder.fetchSource(new String[]{"skuId", "pic", "title", "price"}, null);


        //查询参数

        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }




}
