package com.medical.constant;

public final class DrugConstant {

    private DrugConstant() {}

    public static final Integer DRUG_STATUS_DISABLED = 0;
    public static final Integer DRUG_STATUS_ENABLED = 1;

    public static final String PRICE_TYPE_RETAIL = "RETAIL";
    public static final String PRICE_TYPE_INSURANCE = "INSURANCE";
    public static final String PRICE_TYPE_WHOLESALE = "WHOLESALE";

    public static final String ITEM_TYPE_PRESCRIPTION = "PRESCRIPTION";

    public static final String CACHE_KEY_DRUG_PRICE = "drug:price:%s:%s";
    public static final String CACHE_KEY_DRUG_INFO = "drug:info:%s";

    public static final String FEE_ITEM_NO_PREFIX = "FEE";
    public static final String DRUG_NO_PREFIX = "DRG";
}