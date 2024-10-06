package org.entropy.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

@SpringBootTest
class SearchApplicationTests {

    @Autowired
    private Client searchClient;


    /**
     * 创建单个索引
     * 指定相同的名称创建不会覆盖原有的 primary key
     */
    @Test
    void addIndex() {
        try {
            searchClient.getIndex("movies");
        } catch (MeilisearchApiException e) {
            TaskInfo info = searchClient.createIndex("movies", "id");
            System.out.println(info.getStatus());
        }
    }

    /**
     * 批量创建索引
     */
    @Test
    void addIndexes() {
        IntStream.generate(() -> (int) (Math.random() * 100 + 1))
                .limit(8)
                .parallel()
                .forEach(i ->
                        searchClient.createIndex("test" + i, "id")
                );
    }

    /**
     * 查询指定索引
     */
    @Test
    void queryIndex() {
        try {
            Index index = searchClient.getIndex("movies");
            System.out.println(index);
        } catch (MeilisearchException e) {
            System.err.println("索引不存在");
        }
    }

    /**
     * 查询全部索引
     * 分页限制每页 10 条
     */
    @Test
    void listIndexes() {
        int pageNum = 2;
        int pageSize = 10;
        Results<Index> indexes = searchClient.getIndexes(new IndexesQuery().setOffset((pageNum - 1) * pageSize).setLimit(pageSize));
        Arrays.stream(indexes.getResults()).forEach(System.out::println);
        System.out.println(indexes.getOffset());
        System.out.println(indexes.getLimit());
        System.out.println(indexes.getTotal());
    }

    /**
     * 更新已存在索引的 primary key
     * 索引中不能有数据，否则更新无效
     */
    @Test
    void updateIndex() {
        try {
            Index index = searchClient.getIndex("movies");
            if (index.getStats().getNumberOfDocuments() == 0) {
                TaskInfo info = searchClient.updateIndex("movies", "idx");
                System.out.println(info.getStatus());
            } else {
                System.out.println("文档不为空无法更新 primary key, 数据量: " + index.getStats().getNumberOfDocuments());
            }
        } catch (MeilisearchApiException e) {
            System.err.println("索引不存在");
        }
    }

    /**
     * 删除索引 (不管索引中是否存在数据)
     */
    @Test
    void deleteIndex() {
        TaskInfo info = searchClient.deleteIndex("movies");
        System.out.println(info.getIndexUid());
    }


    /**
     * 添加文档数组或替换它们（如果它们已存在）。如果提供的索引不存在，则将创建该索引
     */
    @Test
    void addOrReplaceDocuments() {
        JSONArray array = new JSONArray();
        ArrayList items = new ArrayList() {{
            add(new JSONObject().put("id", "1").put("title", "你好").put("genres", new JSONArray("[\"Romance\",\"Drama\"]")));
            add(new JSONObject().put("id", "2").put("title", "我的时间").put("genres", new JSONArray("[\"Action\",\"Adventure\"]")));
            add(new JSONObject().put("id", "3").put("title", "Life of Pi").put("genres", new JSONArray("[\"Adventure\",\"Drama\"]")));
            add(new JSONObject().put("id", "4").put("title", "Mad Max: Fury Road").put("genres", new JSONArray("[\"Adventure\",\"Science Fiction\"]")));
            add(new JSONObject().put("id", "5").put("title", "Moana").put("genres", new JSONArray("[\"Fantasy\",\"Action\"]")));
            add(new JSONObject().put("id", "6").put("title", "Philadelphia").put("genres", new JSONArray("[\"Drama\"]")));
        }};
        array.put(items);
        String documents = array.getJSONArray(0).toString();

        // An index is where the documents are stored.
        Index index = searchClient.index("movies");

        // If the index 'movies' does not exist, Meilisearch creates it when you first add the documents.
        index.addDocuments(documents); // => { "taskUid": 0 }
    }

    /**
     * 添加文档列表或更新文档（如果它们已存在）。如果提供的索引不存在，则将创建该索引
     */
    @Test
    void addOrUpdateDocument() {
        JSONArray array = new JSONArray();
        ArrayList items = new ArrayList() {{
            add(new JSONObject().put("id", "6").put("title", "Philadelphia2").put("genres", new JSONArray("[\"Drama2\"]")));
        }};
        array.put(items);
        String documents = array.getJSONArray(0).toString();
        Index index = searchClient.index("movies");
        index.addDocuments(documents);
    }

    /**
     * 查询指定条件的文档数据
     * 需要提前添加筛选属性并使用特定的筛选表达式
     */
    @Test
    void queryDocumentByFilter() {
        searchClient.index("movies").updateSettings(new Settings()
                .setFilterableAttributes(new String[]{"id"}));
        Results<Object> results = searchClient.index("movies").getDocuments(
                new DocumentsQuery()
                        .setFilter(new String[]{"id != 1"})
                        .setFields(new String[]{"id", "title", "genres"})
                        .setLimit(3),
                Object.class
        );
        Arrays.stream(results.getResults()).forEach(System.out::println);
    }

    /**
     * 根据 primary key 值获取文档数据
     */
    @Test
    void getOneDocument() {
        DocumentQuery query = new DocumentQuery();
        query.setFields(new String[]{"title", "genres"});
        Object object = searchClient.index("movies").getDocument("6", query, Object.class);
        System.out.println(object);
    }

    /**
     * 查询所有文档数据
     */
    @Test
    void listDocuments() {
        Results<Object> movies = searchClient.index("movies").getDocuments(Object.class);
        Arrays.stream(movies.getResults()).forEach(System.out::println);
        System.out.println(movies.getTotal());
    }

    /**
     * 删除文档数据
     */
    @Test
    void deleteDocument() {
        TaskInfo info = searchClient.index("movies").deleteDocument("6");
        System.out.println(info.getStatus());
    }

    /**
     * 删除所有文档数据
     */
    @Test
    void deleteAllDocuments() {
        TaskInfo info = searchClient.index("movies").deleteAllDocuments();
        System.out.println(info.getStatus());
    }

    /**
     * 根据筛选器删除文档数据
     */
    @Test
    void deleteDocumentByFilter() {
        searchClient.index("movies").updateSettings(new Settings()
                .setFilterableAttributes(new String[]{"id"}));
        String filter = "id = 3 OR id = 5";
        TaskInfo info = searchClient.index("movies").deleteDocumentsByFilter(filter);
        System.out.println(info.getStatus());
    }


    /**
     * 基础搜索
     */
    @Test
    void basicSearch() {
        SearchResult results = searchClient.index("movies").search("时");
        System.out.println(results);
    }

    /**
     * 自定义条件搜索示例
     */
    @Test
    void customSearch() {
        Index index = searchClient.index("movies");
        index.updateSettings(new Settings()
                .setFilterableAttributes(new String[]{"id"}));
        Searchable results = index.search(
                new SearchRequest("of")
                        .setShowMatchesPosition(true)
                        .setAttributesToHighlight(new String[]{"title"})
                        .setFilter(new String[]{"id = 1"})
        );
        System.out.println(results.getHits());
    }

    @Test
    void customSearch2() {
        Index index = searchClient.index("movies");
        index.updateSettings(new Settings()
                .setFilterableAttributes(new String[]{"id", "title"}));
        Searchable results = index.search(SearchRequest.builder()
                .q("A")
                .offset(0)
                .limit(5)
                .filterArray(new String[][]{
                        new String[]{"id = 2", "id = 4"}, // OR
                        new String[]{"title = 'Mad Max: Fury Road'"} // AND
                })
                .attributesToRetrieve(new String[]{"title", "genres"}) // 返回结果包含的字段
                .attributesToCrop(new String[]{"genres"}) // 裁剪返回结果为指定长度
                .cropLength(1) // 裁剪长度
                .cropMarker("[???]") // 裁剪标记
                .attributesToHighlight(new String[]{"genres"}) // 高亮
                .build());
        System.out.println(results.getHits());
    }
}
