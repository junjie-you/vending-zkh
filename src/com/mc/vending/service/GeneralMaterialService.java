package com.mc.vending.service;

import java.util.Date;
import java.util.List;

import com.mc.vending.data.CardData;
import com.mc.vending.data.ProductCardPowerData;
import com.mc.vending.data.ProductMaterialPowerData;
import com.mc.vending.data.ProductPictureData;
import com.mc.vending.data.StockTransactionData;
import com.mc.vending.data.VendingChnStockData;
import com.mc.vending.data.VendingProLinkData;
import com.mc.vending.db.ProductCardPowerDbOper;
import com.mc.vending.db.ProductMaterialPowerDbOper;
import com.mc.vending.db.ProductPictureDbOper;
import com.mc.vending.db.StockTransactionDbOper;
import com.mc.vending.db.UsedRecordDbOper;
import com.mc.vending.db.VendingChnStockDbOper;
import com.mc.vending.db.VendingProLinkDbOper;
import com.mc.vending.tools.BusinessException;
import com.mc.vending.tools.DateHelper;
import com.mc.vending.tools.ServiceResult;
import com.mc.vending.tools.StringHelper;
import com.mc.vending.tools.ZillionLog;

public class GeneralMaterialService extends BasicService {

    private static GeneralMaterialService instance;

    public static GeneralMaterialService getInstance() {

        if (instance == null) {
            instance = new GeneralMaterialService();
        }
        return instance;
    }

    /**
     * 根据SKUID查询产品图片表记录
     * 
     * @param skuId
     * @return
     */
    public ProductPictureData getPictureBySkuId(String skuId) {
        return new ProductPictureDbOper().getProductPictureBySku(skuId);
    }

    /**
     * 根据售货机ID与售货机货道编号查询库存数量
     * 
     * @param vendingId
     *            售货机ID
     * @param vendingChnCode
     *            售货机货道编号
     * @return
     */
    public int getVendingChnStock(String vendingId, String vendingChnCode) {
        VendingChnStockData vendingChnStock = new VendingChnStockDbOper().getStockByVidAndVcCode(vendingId,
                vendingChnCode);
        if (vendingChnStock == null)
            return 0;
        return vendingChnStock.getVs1Quantity();
    }

    /**
     * 检查产品领料权限
     * 
     * @param vendingId
     *            售货机ID
     * @param skuId
     *            skuId
     * @param cusId
     *            客户ID
     * @param vc2Id
     *            售货机卡/密码权限.ID
     * @param inputQty
     *            输入领料数量
     * @param vendingChnCode
     *            售货机货道号
     * @param cardProductPower  
     *            卡-产品权限类型 （卡-售货机类型，卡-产品类型）
     * @param cardId
     *            卡ID
     * @return
     */
    public ServiceResult<Boolean> checkProductMaterialPower(String vendingId, String skuId, String cusId,
            String vc2Id, int inputQty, String vendingChnCode, String cardProductPower, String cardId) {

        ServiceResult<Boolean> result = new ServiceResult<Boolean>();
        try {
            if (CardData.CARD_PRODUCTPOWER_PRODUCT.equals(cardProductPower)) {
                // 检查卡/产品 领料权限
                this.handlerMaterialPowerProduct(vendingId, skuId, cardId, inputQty, vendingChnCode);
            } else if (CardData.CARD_PRODUCTPOWER_VENDING.equals(cardProductPower)) {
                // 检查卡/密码 领料权限
                this.handlerMaterialPower(vendingId, skuId, cusId, vc2Id, inputQty, vendingChnCode,cardId);
            }

            result.setSuccess(true);
            result.setResult(true);
        } catch (BusinessException be) {
            ZillionLog.e(this.getClass().toString(), "======>>>>检查产品领料权限发生异常",be);
            result.setMessage(be.getMessage());
            result.setCode("1");
            result.setSuccess(false);
        } catch (Exception e) {
            ZillionLog.e(this.getClass().toString(), "======>>>>检查产品领料权限发生异常",e);
            result.setSuccess(false);
            result.setCode("0");
            result.setMessage("售货机系统故障!>>检查产品领料权限发生异常");
        }
        return result;
    }

    /**
     * 处理检查领料权限
     * 
     * @param vendingId
     *            售货机ID
     * @param skuId
     *            skuId
     * @param cusId
     *            客户ID
     * @param vc2Id
     *            售货机卡/密码权限.ID
     * @param inputQty
     *            输入领料数量
     * @param vendingChnCode
     *            售货机货道号
     */
    private void handlerMaterialPower(String vendingId, String skuId, String cusId, String vc2Id,
            int inputQty, String vendingChnCode,String cardId) {

        vc2Id = StringHelper.nullSafeString(vc2Id).trim();
        // f) 根据”售货机卡/密码权限.ID”查询“产品领料权限“表： 如果记录数＝0，继续跳过
        ProductMaterialPowerDbOper productMaterialPowerDbOper = new ProductMaterialPowerDbOper();
        List<String> list = productMaterialPowerDbOper.findVendingProLinkByVcId(vc2Id);
        if (list.isEmpty()) {
            return;
        }
        if (!list.contains(skuId)) {
            throw new BusinessException("输入的卡号或密码无权限领料，请重新输入！");
        }

        VendingProLinkData vendingProLink = new VendingProLinkDbOper().getVendingProLinkByVidAndSkuId(
                vendingId, skuId);
        if (vendingProLink == null) {
            throw new BusinessException("售货机产品 不存在!");
        }
        String vp1Id = vendingProLink.getVp1Id();

        ProductMaterialPowerData productMaterialPower = productMaterialPowerDbOper
                .getVendingProLinkByVidAndSkuId(cusId, vc2Id, vp1Id);

        if (productMaterialPower != null) {
            String pm1Power = productMaterialPower.getPm1Power();
            if (ProductMaterialPowerData.MATERIAL_POWER_NO.equals(pm1Power)) {
                throw new BusinessException("输入的卡号或密码无权限领料,请重新输入!");
            }
            int onceQty = productMaterialPower.getPm1OnceQty();
            String period = productMaterialPower.getPm1Period();
            String intervalStart = productMaterialPower.getPm1IntervalStart();
            String intervalFinish = productMaterialPower.getPm1IntervalFinish();
            String startDate = productMaterialPower.getPm1StartDate();
            int periodQty = productMaterialPower.getPm1PeriodQty();

            if (onceQty == 0 || inputQty <= onceQty) {
                if (!StringHelper.isEmpty(period, true)) {
                    Date date = this.getDate2(period, intervalStart, intervalFinish, startDate);
                    int transQtyTotal = 0;
                    if (date != null) {
                        String dateStr = DateHelper.format(date, "yyyy-MM-dd HH:mm:ss");
                        try {
                            UsedRecordDbOper usedRecordDbOper = new UsedRecordDbOper();
                            transQtyTotal = usedRecordDbOper.getTransQtyCount(cardId, skuId,dateStr);
                            transQtyTotal = transQtyTotal * -1;
                            StockTransactionDbOper stockTransactionDb = new StockTransactionDbOper();
                            int transQtyTotal1 = stockTransactionDb.getTransQtyCount(
                                    StockTransactionData.BILL_TYPE_GET, vendingId, skuId, vendingChnCode,
                                    dateStr, cardId);
                            if (transQtyTotal1 < transQtyTotal) {
                                transQtyTotal = transQtyTotal1;
                            }
                            
                        } catch (Exception e) {
                            ZillionLog.e(this.getClass().toString(), "产品领料权限检查错误",e);
                            // L.e(e.getMessage());
                            // e.printStackTrace();
                            throw new BusinessException("产品领料权限检查错误,库存交易记录数：" + transQtyTotal);
                        }
                    }

                    int total = transQtyTotal * (-1);
                    if (inputQty > periodQty) {
                        throw new BusinessException("你的领料权限是" + periodQty + "，输入了" + inputQty + "，不允许超领！");
                    }
                    if (total + inputQty > periodQty) {
                        throw new BusinessException("你的领料权限是" + periodQty + "，你已领取" + total + "，不允许超领！");
                    }
                }
            } else {
                throw new BusinessException("一次最多领取 " + onceQty + " 数量,请重新输入!");
            }
        }
    }

    /**
     * 处理检查领料权限(卡-产品权限)
     * 
     * @param vendingId
     *            售货机ID
     * @param skuId
     *            skuId
     * @param cusId
     *            客户ID
     * @param vc2Id
     *            售货机卡/密码权限.ID
     * @param inputQty
     *            输入领料数量
     * @param vendingChnCode
     *            售货机货道号
     */
    private void handlerMaterialPowerProduct(String vendingId, String skuId, String cardId, int inputQty,
            String vendingChnCode) {

        ProductCardPowerDbOper dbOper = new ProductCardPowerDbOper();
        
        VendingProLinkData vendingProLink = new VendingProLinkDbOper().getVendingProLinkByVidAndSkuId(
                vendingId, skuId);
        if (vendingProLink == null) {
            throw new BusinessException("售货机产品 不存在!");
        }
//        String vp1Id = vendingProLink.getVp1Id();
        
        List<String> list = dbOper.getVendingProLinkByCid(cardId);
        if (list.isEmpty()) {
            return;
        }
        if (!list.contains(skuId)) {
            throw new BusinessException("输入的卡号或密码无权限领料，请重新输入！");
        }

        ProductCardPowerData powerData = dbOper.getVendingProLinkByVidAndSkuId(cardId, skuId);

        if (powerData != null) {
            String pm1Power = powerData.getPc1Power();
            if (ProductMaterialPowerData.MATERIAL_POWER_NO.equals(pm1Power)) {
                throw new BusinessException("输入的卡号或密码无产品领料权限,请重新输入!");
            }


            int onceQty = 0;
            if (powerData.getPc1OnceQty() == null || powerData.getPc1OnceQty().equals("")) {
            } else {
                onceQty = Integer.valueOf(powerData.getPc1OnceQty());
            }
            String period = powerData.getPc1Period();
            String intervalStart = powerData.getPc1IntervalStart();
            String intervalFinish = powerData.getPc1IntervalFinish();
            String startDate = powerData.getPc1StartDate();
//            String cardId = powerData.getPc1CD1_ID();
            int periodQty = Double.valueOf(powerData.getPc1PeriodQty()).intValue();

            if (onceQty == 0 || inputQty <= onceQty) {
                if (!StringHelper.isEmpty(period, true)) {
                    Date date = this.getDate2(period, intervalStart, intervalFinish, startDate);
                    int transQtyTotal = 0;
                    if (date != null) {
                        String dateStr = DateHelper.format(date, "yyyy-MM-dd HH:mm:ss");
                        try {
                            UsedRecordDbOper usedRecordDbOper = new UsedRecordDbOper();
                            transQtyTotal = usedRecordDbOper.getTransQtyCount(cardId, skuId,dateStr);
                            transQtyTotal = transQtyTotal * -1;
                            StockTransactionDbOper stockTransactionDb = new StockTransactionDbOper();
                            int transQtyTotal1 = stockTransactionDb.getTransQtyCount(
                                    StockTransactionData.BILL_TYPE_GET, vendingId, skuId, vendingChnCode,
                                    dateStr, cardId);
                            if (transQtyTotal1 < transQtyTotal) {
                                transQtyTotal = transQtyTotal1;
                            }

                        } catch (Exception e) {
                            ZillionLog.e(this.getClass().toString(), "产品领料权限检查错误",e);
                            // L.e(e.getMessage());
                             e.printStackTrace();
                            throw new BusinessException("产品领料权限检查错误,产品领用数：" + transQtyTotal);
                        }
                    }

                    int total = transQtyTotal * (-1);
                    if (inputQty > periodQty) {
                        throw new BusinessException("你的领料权限是" + periodQty + "，输入了" + inputQty + "，不允许超领！");
                    }
                    if (total + inputQty > periodQty) {
                        throw new BusinessException("你的领料权限是" + periodQty + "，你已领取" + total + "，不允许超领！");
                    }
                }
            } else {
                throw new BusinessException("一次最多领取 " + onceQty + " 数量,请重新输入!");
            }
        }
    }
}
