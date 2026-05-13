package com.mallfei.cart.application.service;

import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.cart.application.assembler.CartViewAssembler;
import com.mallfei.cart.application.dto.CartAddItemRequest;
import com.mallfei.cart.application.dto.CartCheckoutRequest;
import com.mallfei.cart.application.dto.CartPrepareCheckoutRequest;
import com.mallfei.cart.application.dto.CartSelectItemsRequest;
import com.mallfei.cart.application.dto.CartUpdateItemRequest;
import com.mallfei.cart.application.vo.CartCheckoutValidationItemView;
import com.mallfei.cart.application.vo.CartCheckoutValidationView;
import com.mallfei.cart.application.vo.CartItemView;
import com.mallfei.cart.application.vo.CartListView;
import com.mallfei.cart.application.vo.CartOperationResultView;
import com.mallfei.cart.application.vo.CartQuantityView;
import com.mallfei.cart.application.vo.CartSettlementPreviewItemView;
import com.mallfei.cart.application.vo.CartSettlementPreviewView;
import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.cart.domain.model.ProductSnapshot;
import com.mallfei.cart.domain.service.CartDomainService;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.application.dto.OrderCreateRequest;
import com.mallfei.order.application.vo.OrderDetailView;
import com.mallfei.order.facade.OrderFacade;
import com.mallfei.product.facade.ProductFacade;
import com.mallfei.product.facade.ProductSkuSnapshot;
import com.mallfei.stock.facade.StockFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartApplicationService {

    private final CartDomainService cartDomainService;
    private final OrderFacade orderFacade;
    private final AuthFacade authFacade;
    private final CartViewAssembler cartViewAssembler;
    private final ProductFacade productFacade;
    private final StockFacade stockFacade;

    public CartApplicationService(CartDomainService cartDomainService,
                                  OrderFacade orderFacade,
                                  AuthFacade authFacade,
                                  CartViewAssembler cartViewAssembler,
                                  ProductFacade productFacade,
                                  StockFacade stockFacade) {
        this.cartDomainService = cartDomainService;
        this.orderFacade = orderFacade;
        this.authFacade = authFacade;
        this.cartViewAssembler = cartViewAssembler;
        this.productFacade = productFacade;
        this.stockFacade = stockFacade;
    }

    @Transactional
    public CartOperationResultView addItem(CartAddItemRequest request) {
        Long userId = currentUser().principalId();
        ProductSkuSnapshot snapshot = validatePurchasable(request.skuId(), request.quantity());
        cartDomainService.addItem(userId, snapshot.skuId(), request.quantity(), request.checked());
        return new CartOperationResultView(true, "加入购物车成功", currentQuantity(userId));
    }

    public CartListView listCurrentUserItems() {
        List<CartItem> items = cartDomainService.loadUserCartItems(currentUser().principalId());
        List<CartItemView> views = items.stream().map(this::buildItemView).toList();
        int totalQuantity = items.stream().mapToInt(CartItem::quantity).sum();
        long checkedTotalAmount = views.stream().filter(CartItemView::checked).filter(CartItemView::canCheckout).mapToLong(CartItemView::subtotalAmount).sum();
        return new CartListView(views, views.size(), totalQuantity, checkedTotalAmount);
    }

    @Transactional
    public CartOperationResultView updateItem(Long cartItemId, CartUpdateItemRequest request) {
        if (request.hasNoChanges()) throw BusinessException.badRequest("至少传入一项变更内容");
        Long userId = currentUser().principalId();
        CartItem item = cartDomainService.loadOwnedItem(userId, cartItemId);
        Long targetSkuId = request.skuId() == null ? item.skuId() : request.skuId();
        Integer targetQuantity = request.quantity() == null ? item.quantity() : request.quantity();
        validatePurchasable(targetSkuId, targetQuantity);
        cartDomainService.updateItem(item, targetSkuId, targetQuantity, request.checked());
        return new CartOperationResultView(true, "购物车项编辑成功", currentQuantity(userId));
    }

    @Transactional
    public CartOperationResultView selectItems(CartSelectItemsRequest request) {
        Long userId = currentUser().principalId();
        for (CartItem item : cartDomainService.loadOwnedItems(userId, request.cartItemIds())) cartDomainService.updateItem(item, null, null, request.checked());
        return new CartOperationResultView(true, request.checked() ? "勾选成功" : "取消勾选成功", currentQuantity(userId));
    }

    @Transactional
    public CartOperationResultView deleteItem(Long cartItemId) {
        Long userId = currentUser().principalId();
        cartDomainService.removeItem(userId, cartItemId);
        return new CartOperationResultView(true, "删除成功", currentQuantity(userId));
    }

    @Transactional
    public CartOperationResultView clearCurrentUserCart() {
        Long userId = currentUser().principalId();
        cartDomainService.clear(userId);
        return new CartOperationResultView(true, "清空购物车成功", currentQuantity(userId));
    }

    public CartQuantityView currentQuantity() { return currentQuantity(currentUser().principalId()); }

    public CartSettlementPreviewView settlementPreview() {
        List<CartItem> checkedItems = cartDomainService.loadCheckedItems(currentUser().principalId());
        cartDomainService.ensureCanCheckout(checkedItems);
        List<com.mallfei.order.domain.model.ProductSnapshot> orderSnapshots = checkedItems.stream().map(item -> orderFacade.getProductSnapshot(item.skuId())).toList();
        List<ProductSnapshot> snapshots = orderSnapshots.stream().map(snapshot -> new ProductSnapshot(snapshot.skuId(), snapshot.spuId(), snapshot.skuName(), snapshot.skuImageUrl(), snapshot.salePriceCent())).toList();
        List<CartSettlementPreviewItemView> items = checkedItems.stream().map(item -> {
            ProductSnapshot snapshot = snapshots.stream().filter(candidate -> candidate.skuId().equals(item.skuId())).findFirst().orElseThrow(() -> BusinessException.badRequest("商品SKU不存在: " + item.skuId()));
            return new CartSettlementPreviewItemView(item.id(), item.skuId(), snapshot.skuName(), item.quantity(), snapshot.salePriceCent(), item.subtotalAmount(snapshot.salePriceCent()));
        }).toList();
        long totalAmount = items.stream().mapToLong(CartSettlementPreviewItemView::subtotalAmount).sum();
        return new CartSettlementPreviewView(items, totalAmount, items.size());
    }

    @Transactional
    public CartCheckoutValidationView prepareCheckout(CartPrepareCheckoutRequest request) {
        Long userId = currentUser().principalId();
        List<CartItem> items = cartDomainService.loadOwnedItems(userId, request.cartItemIds());
        for (CartItem item : items) cartDomainService.updateItem(item, null, null, true);
        List<CartCheckoutValidationItemView> validations = items.stream().map(this::validateItemForCheckout).toList();
        boolean passed = validations.stream().allMatch(CartCheckoutValidationItemView::passed);
        long totalAmount = validations.stream().filter(CartCheckoutValidationItemView::passed).mapToLong(item -> item.unitPrice() * item.quantity()).sum();
        return new CartCheckoutValidationView(passed, passed ? "校验通过" : "存在不可结算商品", validations, totalAmount);
    }

    @Transactional
    public OrderDetailView checkout(CartCheckoutRequest request) {
        Long userId = currentUser().principalId();
        List<CartItem> checkedItems = cartDomainService.loadCheckedItems(userId);
        cartDomainService.ensureCanCheckout(checkedItems);
        List<CartCheckoutValidationItemView> validations = checkedItems.stream().map(this::validateItemForCheckout).toList();
        List<CartCheckoutValidationItemView> failedItems = validations.stream().filter(item -> !item.passed()).toList();
        if (!failedItems.isEmpty()) throw BusinessException.badRequest(failedItems.get(0).productName() + " " + failedItems.get(0).message());
        OrderDetailView order = orderFacade.createOrder(new OrderCreateRequest(request.receiverName(), request.receiverPhone(), request.receiverProvinceName(), request.receiverCityName(), request.receiverDistrictName(), request.receiverDetailAddress(), request.remark(), checkedItems.stream().map(item -> new OrderCreateRequest.Item(item.skuId(), item.quantity())).toList()));
        cartDomainService.removeCheckedItems(checkedItems);
        return order;
    }

    private CartItemView buildItemView(CartItem item) {
        try {
            ProductSkuSnapshot snapshot = productFacade.getSkuSnapshot(item.skuId());
            int availableStock = availableStock(item.skuId());
            String invalidReason = checkoutInvalidReason(snapshot, item.quantity(), availableStock);
            return cartViewAssembler.toItemView(item, snapshot, availableStock, invalidReason);
        } catch (BusinessException exception) {
            ProductSkuSnapshot fallback = new ProductSkuSnapshot(item.skuId(), null, "商品已失效", null, null, "未知规格", null, "{}", 0L, 0L, null);
            return cartViewAssembler.toItemView(item, fallback, 0, exception.getMessage());
        }
    }

    private CartCheckoutValidationItemView validateItemForCheckout(CartItem item) {
        try {
            ProductSkuSnapshot snapshot = productFacade.getSkuSnapshot(item.skuId());
            int availableStock = availableStock(item.skuId());
            String invalidReason = checkoutInvalidReason(snapshot, item.quantity(), availableStock);
            return cartViewAssembler.toValidationItem(item, snapshot, invalidReason == null, invalidReason == null ? "校验通过" : invalidReason);
        } catch (BusinessException exception) {
            ProductSkuSnapshot fallback = new ProductSkuSnapshot(item.skuId(), null, "商品已失效", null, null, "未知规格", null, "{}", 0L, 0L, null);
            return cartViewAssembler.toValidationItem(item, fallback, false, exception.getMessage());
        }
    }

    private ProductSkuSnapshot validatePurchasable(Long skuId, Integer quantity) {
        ProductSkuSnapshot snapshot = productFacade.getSkuSnapshot(skuId);
        String invalidReason = checkoutInvalidReason(snapshot, quantity, availableStock(skuId));
        if (invalidReason != null) throw BusinessException.badRequest(invalidReason);
        return snapshot;
    }

    private String checkoutInvalidReason(ProductSkuSnapshot snapshot, Integer quantity, int availableStock) {
        if (snapshot == null) return "商品不存在";
        if (!snapshot.productOnline()) return "商品已下架";
        if (!snapshot.skuOnline()) return "商品规格已下架";
        if (quantity == null || quantity < 1) return "购买数量不合法";
        if (availableStock < quantity) return "库存不足";
        return null;
    }

    private int availableStock(Long skuId) {
        return stockFacade.stockOf(skuId).availableStock();
    }

    private CartQuantityView currentQuantity(Long userId) {
        List<CartItem> items = cartDomainService.loadUserCartItems(userId);
        return new CartQuantityView(items.size(), items.stream().mapToInt(CartItem::quantity).sum());
    }

    private AuthenticatedPrincipal currentUser() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isUser()) throw BusinessException.forbidden("仅用户可访问当前接口");
        return principal;
    }
}
