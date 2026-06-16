package com.mallfei.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {

    @Update("UPDATE oms_order SET order_status = 'PAYMENT_EXCEPTION', expire_time = CASE WHEN order_status = 'PENDING_PAYMENT' THEN NULL ELSE expire_time END, remark = CASE WHEN #{note} IS NULL OR #{note} = '' THEN CASE WHEN remark IS NULL OR remark = '' THEN CONCAT('PAYMENT_EXCEPTION_FROM_', order_status) ELSE CONCAT(remark, ' | PAYMENT_EXCEPTION_FROM_', order_status) END WHEN remark IS NULL OR remark = '' THEN CONCAT('PAYMENT_EXCEPTION_FROM_', order_status, ':', #{note}) ELSE CONCAT(remark, ' | PAYMENT_EXCEPTION_FROM_', order_status, ':', #{note}) END, version = version + 1 WHERE order_no = #{orderNo} AND order_status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED')")
    int markPaymentException(@Param("orderNo") String orderNo, @Param("note") String note);

    @Update("UPDATE oms_order SET order_status = 'PENDING_PAYMENT', expire_time = DATE_ADD(NOW(), INTERVAL 15 MINUTE), remark = CASE WHEN #{note} IS NULL OR #{note} = '' THEN CASE WHEN remark IS NULL OR remark = '' THEN 'RESTORE_PENDING_PAYMENT' ELSE CONCAT(remark, ' | RESTORE_PENDING_PAYMENT') END WHEN remark IS NULL OR remark = '' THEN CONCAT('RESTORE_PENDING_PAYMENT:', #{note}) ELSE CONCAT(remark, ' | RESTORE_PENDING_PAYMENT:', #{note}) END, version = version + 1 WHERE order_no = #{orderNo} AND order_status = 'PAYMENT_EXCEPTION'")
    int restorePendingPayment(@Param("orderNo") String orderNo, @Param("note") String note);

    @Update("UPDATE oms_order SET order_status = 'PAID', paid_at = #{paidAt}, version = version + 1 WHERE order_no = #{orderNo} AND order_status = 'PENDING_PAYMENT'")
    int markPaid(@Param("orderNo") String orderNo, @Param("paidAt") LocalDateTime paidAt);

    @Update("UPDATE oms_order SET order_status = CASE WHEN shipped_at IS NOT NULL OR remark LIKE '%PAYMENT_EXCEPTION_FROM_SHIPPED%' THEN 'SHIPPED' WHEN remark LIKE '%PAYMENT_EXCEPTION_FROM_PROCESSING%' THEN 'PROCESSING' ELSE 'PAID' END, paid_at = COALESCE(paid_at, #{paidAt}), remark = CASE WHEN #{note} IS NULL OR #{note} = '' THEN CASE WHEN remark IS NULL OR remark = '' THEN 'ADMIN_CONFIRM_PAID' ELSE CONCAT(remark, ' | ADMIN_CONFIRM_PAID') END WHEN remark IS NULL OR remark = '' THEN CONCAT('ADMIN_CONFIRM_PAID:', #{note}) ELSE CONCAT(remark, ' | ADMIN_CONFIRM_PAID:', #{note}) END, version = version + 1 WHERE order_no = #{orderNo} AND order_status IN ('PENDING_PAYMENT', 'PAYMENT_EXCEPTION')")
    int markPaidByAdmin(@Param("orderNo") String orderNo, @Param("paidAt") LocalDateTime paidAt, @Param("note") String note);

    @Update("UPDATE oms_order SET order_status = 'TIMEOUT_CANCELLED', cancelled_at = #{now}, version = version + 1 WHERE order_no = #{orderNo} AND order_status = 'PENDING_PAYMENT' AND expire_time <= #{now}")
    int closeTimedOut(@Param("orderNo") String orderNo, @Param("now") LocalDateTime now);

    @Update("UPDATE oms_order SET receiver_name = #{receiverName}, receiver_phone = #{receiverPhone}, receiver_province_name = #{receiverProvinceName}, receiver_city_name = #{receiverCityName}, receiver_district_name = #{receiverDistrictName}, receiver_detail_address = #{receiverDetailAddress}, remark = #{remark}, version = version + 1 WHERE order_no = #{orderNo} AND order_status IN ('PAID', 'PROCESSING')")
    int reviseReceiver(@Param("orderNo") String orderNo,
                       @Param("receiverName") String receiverName,
                       @Param("receiverPhone") String receiverPhone,
                       @Param("receiverProvinceName") String receiverProvinceName,
                       @Param("receiverCityName") String receiverCityName,
                       @Param("receiverDistrictName") String receiverDistrictName,
                       @Param("receiverDetailAddress") String receiverDetailAddress,
                       @Param("remark") String remark);

    @Update("UPDATE oms_order SET receiver_name = #{receiverName}, receiver_phone = #{receiverPhone}, receiver_province_name = #{receiverProvinceName}, receiver_city_name = #{receiverCityName}, receiver_district_name = #{receiverDistrictName}, receiver_detail_address = #{receiverDetailAddress}, remark = CASE WHEN #{note} IS NULL OR #{note} = '' THEN remark WHEN remark IS NULL OR remark = '' THEN #{note} ELSE CONCAT(remark, ' | ', #{note}) END, version = version + 1 WHERE order_no = #{orderNo} AND order_status IN ('PAID', 'PROCESSING')")
    int updateReceiverAddress(@Param("orderNo") String orderNo,
                              @Param("receiverName") String receiverName,
                              @Param("receiverPhone") String receiverPhone,
                              @Param("receiverProvinceName") String receiverProvinceName,
                              @Param("receiverCityName") String receiverCityName,
                              @Param("receiverDistrictName") String receiverDistrictName,
                              @Param("receiverDetailAddress") String receiverDetailAddress,
                              @Param("note") String note);
}
