package com.mallfei.product.application.assembler;

import com.mallfei.product.application.vo.AdminProductDetailView;
import com.mallfei.product.application.vo.AdminProductPageRowView;
import com.mallfei.product.application.vo.AdminProductSkuEditView;
import com.mallfei.product.application.vo.AdminProductSummaryView;
import com.mallfei.product.application.vo.CategoryAdminView;
import com.mallfei.product.application.vo.CategoryTreeNodeView;
import com.mallfei.product.application.vo.ProductCardView;
import com.mallfei.product.application.vo.ProductDetailView;
import com.mallfei.product.application.vo.ProductSkuView;
import com.mallfei.product.domain.model.Category;
import com.mallfei.product.domain.model.ProductSku;
import com.mallfei.product.domain.model.ProductSpu;
import com.mallfei.stock.facade.StockSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProductViewAssembler {

    public List<CategoryTreeNodeView> toCategoryTree(List<Category> categories) {
        List<CategoryTreeNodeView> result = new ArrayList<>();
        for (Category root : categories) {
            if (!root.root()) {
                continue;
            }
            List<CategoryTreeNodeView> children = new ArrayList<>();
            for (Category child : categories) {
                if (!root.id().equals(child.parentId())) {
                    continue;
                }
                children.add(new CategoryTreeNodeView(child.id(), child.name(), List.of()));
            }
            result.add(new CategoryTreeNodeView(root.id(), root.name(), children));
        }
        return result;
    }

    public ProductCardView toProductCard(ProductSpu productSpu) {
        ProductSku defaultSku = productSpu.defaultSku();
        return new ProductCardView(productSpu.id(), productSpu.name(), productSpu.categoryId(), productSpu.mainImageUrl(), defaultSku.salePriceCent(), defaultSku.originPriceCent(), defaultSku.salesCount());
    }

    public ProductDetailView toProductDetail(ProductSpu productSpu) {
        ProductSku defaultSku = productSpu.defaultSku();
        return new ProductDetailView(productSpu.id(), productSpu.name(), productSpu.categoryId(), productSpu.mainImageUrl(), productSpu.albumImagesJson(), productSpu.description() == null ? "" : productSpu.description(), defaultSku.salePriceCent(), defaultSku.originPriceCent(), defaultSku.salesCount(), productSpu.skus().stream().map(this::toSkuView).toList());
    }

    public AdminProductSummaryView toAdminProductSummary(ProductSpu productSpu) {
        return new AdminProductSummaryView(productSpu.id(), productSpu.name(), productSpu.status(), productSpu.skus().size());
    }

    public AdminProductPageRowView toAdminProductPageRow(ProductSpu productSpu, int totalStock) {
        ProductSku defaultSku = productSpu.skus().isEmpty() ? null : productSpu.defaultSku();
        return new AdminProductPageRowView(productSpu.id(), productSpu.name(), productSpu.categoryId(), productSpu.status(), productSpu.skus().size(), defaultSku == null ? 0L : defaultSku.salePriceCent(), totalStock);
    }

    public AdminProductDetailView toAdminProductDetail(ProductSpu productSpu, Map<Long, StockSnapshot> stockBySkuId) {
        return new AdminProductDetailView(productSpu.id(), productSpu.name(), productSpu.categoryId(), productSpu.mainImageUrl(), productSpu.description() == null ? "" : productSpu.description(), productSpu.status(), productSpu.skus().stream().map(sku -> toAdminSkuEditView(sku, stockBySkuId.get(sku.id()))).toList());
    }

    public CategoryAdminView toCategoryAdminView(Category category) {
        return new CategoryAdminView(category.id(), category.name(), category.parentId(), category.level(), category.sortOrder(), category.status());
    }

    private ProductSkuView toSkuView(ProductSku sku) {
        return new ProductSkuView(sku.id(), sku.skuCode(), sku.skuName(), sku.specJson(), sku.salePriceCent(), sku.originPriceCent(), sku.status(), sku.salesCount());
    }

    private AdminProductSkuEditView toAdminSkuEditView(ProductSku sku, StockSnapshot stock) {
        return new AdminProductSkuEditView(sku.id(), sku.skuCode(), sku.skuName(), sku.specJson(), sku.salePriceCent(), sku.originPriceCent(), sku.status(), sku.salesCount(), stock == null ? 0 : stock.totalStock(), stock == null ? 0 : stock.availableStock(), stock == null ? 0 : stock.lockedStock(), stock == null ? "NORMAL" : stock.warningStatus());
    }
}
