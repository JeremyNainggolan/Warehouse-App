package com.example.warehouse.network;

/**
 * This interface contains the URLs used for making network requests in the application.
 */
public interface AppUrl {
    String BASE_URL = "http://103.75.54.70/yim/warehouse/index.php/api/";
    String LOGIN_URL = BASE_URL +  "login/proceed";
    String GET_BATCH = BASE_URL +  "mainn/getbatchinfo/";
    String GET_AREA = BASE_URL + "mainn/getarea/";
    String MOVE_AREA = BASE_URL + "mainn/move";
    String CHECK = BASE_URL + "mainn/check/";
    String STOCK_TAKE = BASE_URL + "mainn/stocktake/";
}
