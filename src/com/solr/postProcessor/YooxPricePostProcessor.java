/*     */ package com.solr.postProcessor;
/*     */ 
/*     */ /*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.LinkedList;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.logging.Logger;

/*     */ import org.apache.commons.json.JSONArray;
/*     */ import org.apache.commons.json.JSONException;
/*     */ import org.apache.commons.json.JSONObject;
/*     */ import org.apache.wink.common.http.HttpStatus;

import com.ibm.commerce.foundation.common.util.logging.LoggingHelper;
/*     */ import com.ibm.commerce.foundation.internal.client.contextservice.RemoteCallException;
/*     */ import com.ibm.commerce.foundation.internal.client.util.RestHandlerHelper;
/*     */ import com.ibm.commerce.foundation.internal.server.services.search.config.solr.SolrSearchConfigurationRegistry;
/*     */ import com.ibm.commerce.foundation.internal.server.services.search.util.EntitlementHelper;
/*     */ import com.ibm.commerce.foundation.internal.server.services.search.util.RemoteRestCallHelper;
/*     */ import com.ibm.commerce.foundation.internal.server.services.search.util.StoreHelper;
/*     */ import com.ibm.commerce.foundation.server.services.dataaccess.SelectionCriteria;
/*     */ import com.ibm.commerce.foundation.server.services.rest.search.SearchCriteria;
/*     */ import com.ibm.commerce.foundation.server.services.search.query.SearchQueryPostprocessor;
/*     */ import com.ibm.commerce.foundation.server.services.search.query.solr.AbstractSolrSearchQueryPostprocessor;
/*     */ import com.ibm.commerce.foundation.server.services.valuemapping.ValueMappingService;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class YooxPricePostProcessor extends AbstractSolrSearchQueryPostprocessor
/*     */   implements SearchQueryPostprocessor
/*     */ {
/*     */   private static final String COPYRIGHT = "(c) Copyright International Business Machines Corporation 1996,2008";
/*  61 */   private static final String CLASSNAME = YooxPricePostProcessor.class
/*  61 */     .getName();
/*     */ 
/*  63 */   private static final Logger LOGGER = LoggingHelper.getLogger(YooxPricePostProcessor.class);
/*     */   private static final int URL_LEN = 200;
/*     */   private static final String STR_RESOURCE_PREFIX = "/wcs/resources";
/*     */   private static final String STR_STORE_URL = "/store/";
/*     */   private static final String STR_PRICE_URL = "/price";
/*     */   private static final String STR_SLASH = "/";
/*     */   private static final String CATALOG_ENTRY_VIEW_PRICE_VALUE = "value";
/*     */   private static final String CATALOG_ENTRY_VIEW_PRICE_CURRENCY = "currency";
/*     */   private static final String CATALOG_ENTRY_VIEW_PRICE_DESCRIPTION = "description";
/*     */   private static final String CATALOG_ENTRY_VIEW_PRICE_USAGE = "usage";
/*     */   private static final String CATALOG_ENTRY_VIEW_PRICE_CONTRACTID = "contractId";
/*  82 */   private ValueMappingService iMappingService = null;
/*  83 */   private String iCatalogEntryViewMapper = null;
/*  84 */   private String iPriceMapper = null;
/*     */ 
/*  86 */   private String iCatalogEntryViewName = null;
/*  87 */   private String iCatalogEntryIdName = null;
/*  88 */   private String iCatalogEntryGroupViewName = null;
/*     */ 
/*  90 */   private String iPriceName = null;
/*  91 */   private String iPriceValueName = null;
/*  92 */   private String iPriceCurrencyName = null;
/*  93 */   private String iPriceDescriptionName = null;
/*  94 */   private String iPriceUsageName = null;
/*  95 */   private String iPriceContractId = null;
/*     */ 
/*  97 */   private String iCurrencyCode = null;
/*  98 */   private String iStoreId = null;
/*  99 */   private String iContractId = null;
/*     */ 
/* 101 */   private String iComponentId = null;
/* 102 */   private SolrSearchConfigurationRegistry iSearchRegistry = null;
/*     */ 
/*     */   public YooxPricePostProcessor(String componentId)
/*     */   {
/* 115 */     this.iComponentId = componentId;
/*     */   }
/*     */ 
/*     */   public void invoke(SelectionCriteria selectionCriteria, Object[] queryResponseObjects)
/*     */     throws RuntimeException
/*     */   {
/* 124 */     String METHODNAME = "invoke(SelectionCriteria, Object[])";
/* 125 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 126 */       LOGGER.entering(CLASSNAME, "invoke(SelectionCriteria, Object[])", new Object[] { 
/* 127 */         selectionCriteria, queryResponseObjects });
/*     */     }
/*     */ 
/* 130 */     super.invoke(selectionCriteria, queryResponseObjects);
/* 131 */     this.iSearchRegistry = SolrSearchConfigurationRegistry.getInstance(this.iComponentId);
/* 132 */     String searchProfile = getControlParameterValue("_wcf.search.profile");
/* 133 */     initContextParameters();
/* 134 */     initMappingParameters();
/*     */ 
/* 136 */     String priceModeFromURL = getControlParameterValue("_wcf.search.price");
/* 137 */     if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 138 */       LOGGER
/* 139 */         .logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 140 */         CLASSNAME, "invoke(SelectionCriteria, Object[])", 
/* 141 */         "PriceMode got from url is: " + priceModeFromURL);
/*     */     }
/*     */ 
/* 146 */     String priceMode = "1";
/* 147 */     String storeId = getControlParameterValue("_wcf.search.store.online");
/* 148 */     boolean bNeedPriceREST = false;
/* 149 */     if (priceModeFromURL != null)
/*     */     {
/* 151 */       if (("calculated"
/* 151 */         .equals(priceModeFromURL)) || 
/* 153 */         ("mixed"
/* 153 */         .equals(priceModeFromURL))) {
/* 154 */         bNeedPriceREST = true;
/*     */       }
/* 156 */       priceMode = translatePriceMode(priceModeFromURL);
/*     */     } else {
/* 158 */       priceMode = StoreHelper.getPriceMode(searchProfile, storeId);
/* 159 */       if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 160 */         LOGGER
/* 161 */           .logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 162 */           CLASSNAME, "invoke(SelectionCriteria, Object[])", 
/* 163 */           "PriceMode got from store is: " + priceMode);
/*     */       }
/*     */     }
/*     */ 
/* 167 */     if (priceMode != null) {
/* 168 */       Map metaData = 
/* 169 */         (Map)this.iSearchResponseObject
/* 169 */         .getResponse().get(
/* 170 */         "metaData");
/* 171 */       if (metaData == null) {
/* 172 */         metaData = new LinkedHashMap();
/* 173 */         this.iSearchResponseObject.getResponse().put(
/* 174 */           "metaData", metaData);
/*     */       }
/* 176 */       metaData.put("price", 
/* 177 */         priceMode);
/*     */     }
/*     */ 
/* 182 */     List<Map> catalogEntryViews = 
/* 183 */       (LinkedList)this.iSearchResponseObject
/* 183 */       .getResponse().get(this.iCatalogEntryViewName);
/*     */ 
/* 186 */     if ((catalogEntryViews == null) || (catalogEntryViews.isEmpty())) {
/* 187 */       Map catalogEntryGroups = 
/* 188 */         (Map)this.iSearchResponseObject
/* 188 */         .getResponse().get(this.iCatalogEntryGroupViewName);
/* 189 */       if ((catalogEntryGroups != null) && 
/* 190 */         (catalogEntryGroups.size() > 0)) {
/* 191 */         catalogEntryViews = new LinkedList();
/*     */ 
/* 193 */         Iterator localIterator1 = catalogEntryGroups
/* 193 */           .entrySet().iterator();
/*     */ 
/* 192 */         while (localIterator1.hasNext()) {
/* 193 */           Map.Entry catalogEntryGroup = (Map.Entry)localIterator1.next();
/* 194 */           catalogEntryViews.addAll(
/* 195 */             (Collection)catalogEntryGroup.getValue());
/*     */         }
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 201 */     String offerPriceFieldNamePrefix = "price_";
/* 202 */     String listPriceFieldName = "listprice_";
/* 203 */     String priceFieldName = null;
/* 204 */     if (this.iCurrencyCode != null)
/*     */     {
/* 207 */       listPriceFieldName = listPriceFieldName.concat(this.iCurrencyCode).concat("1000000000");
/*     */     }
/*     */ 
/* 210 */     if ((catalogEntryViews != null) && (!(catalogEntryViews.isEmpty()))) {
/* 211 */       Map pricesServiceResult = null;
/*     */ 
/* 214 */       if ((bNeedPriceREST) && 
/* 216 */         (catalogEntryViews.size() > 0)) {
/* 217 */         pricesServiceResult = getPriceRest(catalogEntryViews.size(), 
/* 218 */           catalogEntryViews, selectionCriteria);
/*     */       }
/*     */ 
/* 221 */       for (Map catalogEntryView : catalogEntryViews) {
/* 222 */         List finalPriceList = new ArrayList();
/* 223 */         Map finalPriceMap = new HashMap();
/*     */ 
/* 225 */         if (catalogEntryView.containsKey(listPriceFieldName)) {
/* 226 */           Object priceValue = catalogEntryView.remove(listPriceFieldName);
/* 227 */           finalPriceMap.put(this.iPriceValueName, priceValue);
/*     */         } else {
/* 229 */           finalPriceMap.put(this.iPriceValueName, "");
/*     */         }
/* 231 */         finalPriceMap
/* 232 */           .put(
/* 233 */           this.iPriceDescriptionName, "L");
/* 234 */         finalPriceMap.put(this.iPriceCurrencyName, this.iCurrencyCode);
/* 235 */         finalPriceMap.put(this.iPriceUsageName, 
/* 236 */           "Display");
/* 237 */         finalPriceList.add(finalPriceMap);
/*     */ 
/* 240 */         if ("1"
/* 240 */           .equals(priceMode)) {
/* 241 */           List<String> fieldNameWithPatternList = applyFieldNamingPattern(offerPriceFieldNamePrefix, this.iCurrencyCode);
/* 242 */           for (String priceFieldNameWithPattern : fieldNameWithPatternList)
/*     */           {
/* 244 */             finalPriceMap = new HashMap();
/* 245 */             String contractId = getAppliedContractIdFromPriceField(priceFieldNameWithPattern);
/* 246 */             if (catalogEntryView.containsKey(priceFieldNameWithPattern)) {
/* 247 */               Object priceValue = catalogEntryView
/* 248 */                 .remove(priceFieldNameWithPattern);
/* 249 */               finalPriceMap.put(this.iPriceValueName, priceValue);
/* 250 */               if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 251 */                 LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 252 */                   CLASSNAME, "invoke(SelectionCriteria, Object[])", "Value for " + priceFieldName + " is: " + 
/* 253 */                   priceValue);
/*     */               }
/* 255 */               finalPriceMap.put(this.iPriceDescriptionName, "I");
/* 256 */               finalPriceMap.put(this.iPriceCurrencyName, this.iCurrencyCode);
/* 257 */               finalPriceMap.put(this.iPriceUsageName, 
/* 258 */                 "Offer");
/* 259 */               if (contractId != null) {
/* 260 */                 finalPriceMap.put(this.iPriceContractId, contractId);
/*     */               }
/* 262 */               finalPriceList.add(finalPriceMap);
/*     */             }
/*     */             else {
/* 265 */               finalPriceMap.put(this.iPriceValueName, "");
/* 266 */               if (contractId != null) {
/* 267 */                 finalPriceMap.put(this.iPriceContractId, contractId);
/*     */               }
/* 269 */               finalPriceList.add(finalPriceMap);
/*     */             }
/*     */           }
/*     */         } else {
/* 273 */           finalPriceMap = new HashMap();
/*     */ 
/* 275 */           if (pricesServiceResult != null)
/* 276 */             finalPriceMap.put(this.iPriceValueName, 
/* 277 */               pricesServiceResult.get(catalogEntryView.get(this.iCatalogEntryIdName)));
/*     */           else {
/* 279 */             finalPriceMap.put(this.iPriceValueName, "");
/*     */           }
/* 281 */           finalPriceMap.put(this.iPriceDescriptionName, "O");
/* 282 */           finalPriceMap.put(this.iPriceCurrencyName, this.iCurrencyCode);
/* 283 */           finalPriceMap.put(this.iPriceUsageName, 
/* 284 */             "Offer");
/* 285 */           finalPriceList.add(finalPriceMap);
/*     */         }
/* 287 */         catalogEntryView.put(this.iPriceName, finalPriceList);
/*     */       }
/*     */     }
/*     */ 
/* 291 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
/* 292 */       LOGGER.exiting(CLASSNAME, "invoke(SelectionCriteria, Object[])");
/*     */   }
/*     */ 
/*     */   private Map<String, String> getPriceRest(int entrySize, List<Map> catalogEntryViews, SelectionCriteria selectionCriteria)
/*     */   {
/* 299 */     String METHODNAME = "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)";
/* 300 */     boolean traceOn = LoggingHelper.isTraceEnabled(LOGGER);
/* 301 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 302 */       LOGGER.entering(CLASSNAME, "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)", new Object[] { 
/* 303 */         Integer.valueOf(entrySize), catalogEntryViews, selectionCriteria });
/*     */     }
/*     */ 
/* 306 */     this.iContractId = EntitlementHelper.getContractFromRemoteOrLocal((SearchCriteria)selectionCriteria);
/* 307 */     StringBuffer contentJSON = new StringBuffer();
/* 308 */     contentJSON
/* 309 */       .append("{\"query\":{\"name\":\"byProductID\",\"products\":[");
/* 310 */     boolean isFirst = true;
/* 311 */     for (Map catalogEntryView : catalogEntryViews) {
/* 312 */       if (!(isFirst))
/* 313 */         contentJSON.append(",");
/*     */       else {
/* 315 */         isFirst = false;
/*     */       }
/* 317 */       contentJSON
/* 318 */         .append("{\"contractIds\":[\"").append(this.iContractId).append("\"]").append(",")
/* 319 */         .append("\"currencies\":[\"").append(this.iCurrencyCode).append("\"]").append(",")
/* 320 */         .append("\"productId\": \"").append(catalogEntryView.get(this.iCatalogEntryIdName)).append("\"")
/* 321 */         .append("}");
/*     */     }
/* 323 */     contentJSON.append("]}}");
/*     */ 
/* 326 */     String strURL = buildURLToGetPrice(this.iStoreId);
/* 327 */     JSONObject response = null;
/* 328 */     Map prices = new HashMap();
/*     */     try {
/* 330 */       int expectedReturnCode = HttpStatus.OK.getCode();
/* 331 */       response = 
/* 332 */         RemoteRestCallHelper.issueRESTServiceRequest(strURL, contentJSON.toString(), 
/* 333 */         expectedReturnCode);
/*     */ 
/* 335 */       if ((response != null) && (!(response.isEmpty()))) {
/* 336 */         if (traceOn) {
/* 337 */           LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 338 */             CLASSNAME, "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)", "Price service response: " + 
/* 339 */             response.toString());
/*     */         }
/* 341 */         JSONArray entryPrices = response
/* 342 */           .getJSONArray("EntitledPrice");
/* 343 */         for (int i = 0; i < entryPrices.size(); ++i) {
/* 344 */           JSONObject entryPrice = entryPrices.getJSONObject(i);
/* 345 */           String entryId = entryPrice.getString("productId");
/* 346 */           JSONArray unitPrices = entryPrice.getJSONArray("UnitPrice");
/* 347 */           if (unitPrices.size() > 0) {
/* 348 */             String unitpric = unitPrices.getJSONObject(0)
/* 349 */               .getJSONObject("price").getString("value");
/* 350 */             prices.put(entryId, unitpric);
/*     */           }
/*     */         }
/* 343 */         
/*     */       }
/*     */ 
/* 354 */       if (traceOn)
/* 355 */         LOGGER
/* 356 */           .logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 357 */           CLASSNAME, "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)", 
/* 358 */           "Marketing component did not return any results from search activities.");
/*     */     }
/*     */     catch (JSONException e)
/*     */     {
/* 362 */       if (traceOn)
/* 363 */         LOGGER
/* 364 */           .logp(
/* 365 */           LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, 
/* 366 */           CLASSNAME, 
/* 367 */           "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)", 
/* 368 */           "Exception occurred when parsing the JSON response from the marketing service:  " + 
/* 369 */           e.getMessage());
/*     */     }
/*     */     catch (RemoteCallException e) {
/* 372 */       if (traceOn) {
/* 373 */         LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, 
/* 374 */           "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)", 
/* 375 */           "Exception occurred when calling the price service:  " + 
/* 376 */           e.getMessage());
/*     */       }
/*     */     }
/*     */ 
/* 380 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
LOGGER.exiting(CLASSNAME, "getPriceRest(int, List<Map<String, Object>>, selectionCriteria)");
/*     */     }
/* 383 */     return prices;
/*     */   }
/*     */ 
/*     */   private static String buildURLToGetPrice(String astrStoreId)
/*     */   {
/* 396 */     String METHODNAME = "buildURLToGetPrice";
/* 397 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 398 */       LOGGER.entering(CLASSNAME, "buildURLToGetPrice", astrStoreId);
/*     */     }
/* 400 */     StringBuilder sbUrl = new StringBuilder(200);
/*     */ 
/* 403 */     boolean isSecured = RemoteRestCallHelper.isRequestSecure();
/*     */ 
/* 406 */     String strPriceServiceHostNamePort = RestHandlerHelper.getInstance()
/* 407 */       .getURLProtocolAndHostname(isSecured, false);
/*     */ 
/* 410 */     sbUrl.append(strPriceServiceHostNamePort).append("/wcs/resources")
/* 411 */       .append("/store/").append(astrStoreId)
/* 412 */       .append("/price");
/*     */ 
/* 414 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 415 */       LOGGER.exiting(CLASSNAME, "buildURLToGetPrice", sbUrl.toString());
/*     */     }
/* 417 */     return sbUrl.toString();
/*     */   }
/*     */ 
/*     */   private void initContextParameters()
/*     */   {
/* 422 */     String METHODNAME = "initContextParameters";
/* 423 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
/* 424 */       LOGGER.entering(CLASSNAME, "initContextParameters");
/*     */     try
/*     */     {
/* 427 */       this.iCurrencyCode = getFinalControlParameterValue("_wcf.search.currency");
/* 428 */       if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 429 */         LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, 
/* 430 */           "initContextParameters", "Currency Code=" + this.iCurrencyCode);
/*     */       }
/* 432 */       this.iStoreId = getFinalControlParameterValue("_wcf.search.store.online");
/*     */     }
/*     */     catch (Exception e) {
/* 435 */       throw new RuntimeException(e);
/*     */     }
/*     */ 
/* 438 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
/* 439 */       LOGGER.exiting(CLASSNAME, "initContextParameters");
/*     */   }
/*     */ 
/*     */   protected void initMappingParameters()
/*     */   {
/* 447 */     String METHODNAME = "initMappingParameters";
/* 448 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 449 */       LOGGER.entering(CLASSNAME, "initMappingParameters", new Object[0]);
/*     */     }
/* 451 */     this.iMappingService = ValueMappingService.getInstance(
/* 452 */       this.iSelectionCriteria.getComponentId());
/* 453 */     SearchCriteria searchCriteria = new SearchCriteria(this.iSelectionCriteria);
/* 454 */     String resourceName = searchCriteria
/* 455 */       .getControlParameterValue("_wcf.search.internal.service.resource");
/* 456 */     this.iCatalogEntryViewMapper = 
/* 457 */       getMapperName(searchCriteria, resourceName, 
/* 457 */       "CatalogEntryView");
/*     */ 
/* 459 */     this.iCatalogEntryViewName = 
/* 461 */       getExternalFieldName(this.iMappingService, 
/* 460 */       this.iCatalogEntryViewMapper, 
/* 461 */       "catalogEntryView");
/* 462 */     this.iCatalogEntryIdName = 
/* 464 */       getExternalFieldName(this.iMappingService, 
/* 463 */       this.iCatalogEntryViewMapper, 
/* 464 */       "catentry_id");
/* 465 */     this.iCatalogEntryGroupViewName = 
/* 467 */       getExternalFieldName(this.iMappingService, 
/* 466 */       "XPathToGroupingBODResponseFieldNameMapping", 
/* 467 */       "catalogEntryGroupView");
/*     */ 
/* 470 */     this.iPriceMapper = getMapperName(
/* 471 */       searchCriteria, 
/* 472 */       resourceName, 
/* 473 */       "CatalogEntryView/Price");
/*     */ 
/* 476 */     this.iPriceName = 
/* 479 */       getExternalFieldName(this.iMappingService, 
/* 478 */       this.iPriceMapper, 
/* 479 */       "price");
/* 480 */     this.iPriceValueName = 
/* 481 */       getExternalFieldName(this.iMappingService, this.iPriceMapper, 
/* 481 */       "value");
/* 482 */     this.iPriceDescriptionName = 
/* 483 */       getExternalFieldName(this.iMappingService, 
/* 483 */       this.iPriceMapper, "description");
/* 484 */     this.iPriceCurrencyName = 
/* 485 */       getExternalFieldName(this.iMappingService, 
/* 485 */       this.iPriceMapper, "currency");
/* 486 */     this.iPriceUsageName = 
/* 487 */       getExternalFieldName(this.iMappingService, this.iPriceMapper, 
/* 487 */       "usage");
/* 488 */     this.iPriceContractId = 
/* 489 */       getExternalFieldName(this.iMappingService, this.iPriceMapper, 
/* 489 */       "contractId");
/*     */ 
/* 491 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER))
/* 492 */       LOGGER.exiting(CLASSNAME, "initMappingParameters");
/*     */   }
/*     */ 
/*     */   private String translatePriceMode(String paramPriceMode)
/*     */   {
/* 499 */     String METHODNAME = "translatePriceMode(String paramPriceMode)";
/* 500 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 501 */       LOGGER.entering(CLASSNAME, "translatePriceMode(String paramPriceMode)", new Object[] { paramPriceMode });
/*     */     }
/* 503 */     String priceModeInControlParameter = "1";
/*     */ 
/* 505 */     if ("calculated"
/* 505 */       .equals(paramPriceMode)) {
/* 506 */       priceModeInControlParameter = "0";
/*     */     }
/* 509 */     else if ("mixed"
/* 509 */       .equals(paramPriceMode)) {
/* 510 */       priceModeInControlParameter = "2";
/*     */     }
/*     */     else
/*     */     {
/* 514 */       priceModeInControlParameter = "1";
/*     */     }
/* 516 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 517 */       LOGGER.exiting(CLASSNAME, "translatePriceMode(String paramPriceMode)", priceModeInControlParameter);
/*     */     }
/* 519 */     return priceModeInControlParameter;
/*     */   }
/*     */ 
/*     */   protected List<String> applyFieldNamingPattern(String origFieldName, String currencyCode)
/*     */   {
/* 532 */     String METHODNAME = "applyFieldNamingPattern(String origFieldName String currencyCode)";
/* 533 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 534 */       LOGGER.entering(CLASSNAME, "applyFieldNamingPattern(String origFieldName String currencyCode)", new Object[] { origFieldName, currencyCode });
/*     */     }
/* 536 */     ArrayList fieldNameWithPattern = new ArrayList();
/* 537 */     if (origFieldName == null) {
/* 538 */       return null;
/*     */     }
/* 540 */     String storeId = getFinalControlParameterValue("_wcf.search.store.online");
/* 541 */     String compatiblePriceIndexMode = StoreHelper.getCompatiblePriceIndexMode(storeId);
/* 542 */     if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 543 */       if (compatiblePriceIndexMode != null)
/* 544 */         LOGGER.exiting(CLASSNAME, "applyFieldNamingPattern(String origFieldName String currencyCode)", "compatiblePriceIndexMode:" + compatiblePriceIndexMode);
/*     */       else {
/* 546 */         LOGGER.exiting(CLASSNAME, "applyFieldNamingPattern(String origFieldName String currencyCode)", "Not define compatiblePriceIndexMode, will use compatible mode");
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 553 */     if ((compatiblePriceIndexMode != null) && 
/* 554 */       ("1.0".equals(compatiblePriceIndexMode)))
/*     */     {
/* 556 */       String strContractId = EntitlementHelper.getFinalUsableContract((SearchCriteria)this.iSelectionCriteria);
/* 557 */       List<String> contractIdList = EntitlementHelper.getContractIdAsStringListFromString(strContractId);
/* 558 */       for (String contract : contractIdList) {
/* 559 */         String newFieldName = origFieldName.concat(currencyCode).concat("_").concat(contract);
/* 560 */         fieldNameWithPattern.add(newFieldName);
/* 561 */         if (LoggingHelper.isTraceEnabled(LOGGER))
/* 562 */           LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, 
/* 563 */             "applyFieldNamingPattern(String origFieldName String currencyCode)", "create new facet name with: " + newFieldName);
/*     */       }
/*     */     }
/*     */     else {
/* 567 */       String newFieldName = origFieldName.concat(currencyCode);
/* 568 */       fieldNameWithPattern.add(newFieldName);
/* 569 */       if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 570 */         LOGGER.logp(LoggingHelper.DEFAULT_TRACE_LOG_LEVEL, CLASSNAME, 
/* 571 */           "applyFieldNamingPattern(String origFieldName String currencyCode)", "applied facet pattern: " + newFieldName);
/*     */       }
/*     */     }
/*     */ 
/* 575 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 576 */       LOGGER.exiting(CLASSNAME, "applyFieldNamingPattern(String origFieldName String currencyCode)");
/*     */     }
/* 578 */     return fieldNameWithPattern;
/*     */   }
/*     */ 
/*     */   protected String getAppliedContractIdFromPriceField(String priceFieldName)
/*     */   {
/* 588 */     String METHODNAME = "getAppliedContractIdFromPriceField(String priceFieldName)";
/* 589 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 590 */       LOGGER.entering(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)", new Object[] { priceFieldName });
/*     */     }
/* 592 */     String contractId = null;
/*     */ 
/* 594 */     if ((priceFieldName != null) && (priceFieldName.startsWith("price_"))) {
/* 595 */       String storeId = getFinalControlParameterValue("_wcf.search.store.online");
/* 596 */       String compatiblePriceIndexMode = StoreHelper.getCompatiblePriceIndexMode(storeId);
/* 597 */       if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 598 */         if (compatiblePriceIndexMode != null)
/* 599 */           LOGGER.exiting(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)", "compatiblePriceIndexMode:" + compatiblePriceIndexMode);
/*     */         else {
/* 601 */           LOGGER.exiting(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)", "Not define compatiblePriceIndexMode, will use compatible mode");
/*     */         }
/*     */ 
/*     */       }
/*     */ 
/* 608 */       if ((compatiblePriceIndexMode != null) && 
/* 609 */         ("1.0".equals(compatiblePriceIndexMode))) {
/* 610 */         int index = priceFieldName.lastIndexOf("_");
/* 611 */         contractId = priceFieldName.substring(index + 1);
/* 612 */         if (LoggingHelper.isTraceEnabled(LOGGER)) {
/* 613 */           if (contractId != null)
/* 614 */             LOGGER.exiting(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)", "contractId:" + contractId);
/*     */           else {
/* 616 */             LOGGER.exiting(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)", "Not contractId in priceFieldName");
/*     */           }
/*     */         }
/*     */       }
/*     */     }
/* 621 */     if (LoggingHelper.isEntryExitTraceEnabled(LOGGER)) {
/* 622 */       LOGGER.exiting(CLASSNAME, "getAppliedContractIdFromPriceField(String priceFieldName)");
/*     */     }
/* 624 */     return contractId;
/*     */   }
/*     */ }

/* Location:           K:\IBM\WCDE7009\workspace\Search\Foundation-Server-FEP.jar
 * Qualified Name:     com.ibm.commerce.foundation.server.services.rest.search.postprocessor.solr.SolrRESTSearchCatalogEntryViewPriceQueryPostprocessor
 * Java Class Version: 6 (50.0)
 * JD-Core Version:    0.5.3
 */
