package com.mallfei.cart.application.assembler;

import com.mallfei.cart.application.vo.CartCheckoutValidationItemView;
import com.mallfei.cart.application.vo.CartItemView;
import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.product.facade.ProductSkuSnapshot;
import org.springframework.stereotype.Component;

@Component
public class CartViewAssembler {

    public CartItemView toItemView(CartItem item,
                                   ProductSkuSnapshot snapshot,
                                   int availableStock,
                                   String invalidReason) {
        long unitPrice = snapshot.salePriceCent() == null ? 0L : snapshot.salePriceCent();
        return new CartItemView(
                item.id(),
                item.skuId(),
                snapshot.spuId(),
                snapshot.spuName(),
                snapshot.mainImageUrl(),
                snapshot.skuName(),
                snapshot.specJson(),
                unitPrice,
                item.quantity(),
                item.subtotalAmount(unitPrice),
                item.checkedForCheckout(),
                invalidReason == null,
                invalidReason,
                item.createdAt()
        );
    }

    public CartCheckoutValidationItemView toValidationItem(CartItem item,
                                                           ProductSkuSnapshot snapshot,
                                                           boolean passed,
                                                           String message) {
        return new CartCheckoutValidationItemView(
                item.id(),
                item.skuId(),
                snapshot.spuName(),
                snapshot.skuName(),
                item.quantity(),
                snapshot.salePriceCent(),
                passed,
                message
        );
    }
}
