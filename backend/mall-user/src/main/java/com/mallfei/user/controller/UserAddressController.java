package com.mallfei.user.controller;

import com.mallfei.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/address")
public class UserAddressController {

    @GetMapping("/list")
    public R<?> list() {
        return R.ok(List.of(
                Map.of(
                        "id", 1,
                        "receiverName", "示例用户",
                        "receiverPhone", "13800000000",
                        "detailAddress", "示例地址"
                )
        ));
    }
}
