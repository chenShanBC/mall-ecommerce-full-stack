<template>
  <div class="page">
    <van-nav-bar :title="isSelectMode ? '选择收货地址' : '收货地址'" left-arrow @click-left="handleBack" />

    <div class="list-wrap">
      <van-empty v-if="!addresses.length" description="暂无收货地址" />
      <div
        v-for="item in addresses"
        :key="item.id"
        class="address-card"
        :class="{
          'default-card': item.default,
          'selectable-card': isSelectMode,
          'selected-card': isSelectMode && String(selectedAddressId) === String(item.id),
        }"
        @click="handleAddressClick(item)"
      >
        <div class="address-head">
          <div class="contact-wrap">
            <span v-if="item.default" class="default-badge">默认地址</span>
            <span v-if="isSelectMode && String(selectedAddressId) === String(item.id)" class="selected-badge">当前选择</span>
            <span class="name">{{ item.receiverName }}</span>
            <span class="phone">{{ item.receiverPhone }}</span>
          </div>
          <van-icon v-if="item.default && !isSelectMode" name="passed" class="default-icon" />
          <van-icon v-if="isSelectMode && String(selectedAddressId) === String(item.id)" name="success" class="selected-icon" />
        </div>
        <div class="detail">{{ formatAddress(item) }}</div>
        <div v-if="!isSelectMode" class="actions">
          <van-button size="small" plain type="primary" @click.stop="openEdit(item)">编辑</van-button>
          <van-button
            size="small"
            plain
            :type="item.default ? 'success' : 'default'"
            :disabled="item.default"
            @click.stop="makeDefault(item)"
          >
            {{ item.default ? '当前默认地址' : '设为默认' }}
          </van-button>
          <van-button v-if="!item.default" size="small" plain type="danger" @click.stop="removeAddress(item)">删除</van-button>
        </div>
      </div>
    </div>

    <div class="bottom-action">
      <van-button block round type="primary" @click="openCreate">新增地址</van-button>
    </div>

    <van-popup v-model:show="showPopup" position="bottom" round :style="{ height: '78%' }">
      <div class="popup-body">
        <div class="popup-title">{{ form.id ? '编辑地址' : '新增地址' }}</div>
        <van-form @submit="submitForm">
          <van-cell-group inset>
            <van-field v-model="form.receiverName" label="收件人" placeholder="请输入收件人姓名" />
            <van-field v-model="form.receiverPhone" label="手机号" placeholder="请输入手机号" maxlength="11" />
            <van-field
              :model-value="regionText"
              label="所在地区"
              placeholder="请选择省/市/区"
              readonly
              is-link
              @click="showAreaPicker = true"
            />
            <van-field v-model="form.detailAddress" label="详细地址" placeholder="请输入详细地址" />
            <van-field v-model="form.postalCode" label="邮编" placeholder="请输入邮编" />
            <van-switch-cell v-model="form.isDefault" title="设为默认地址" />
          </van-cell-group>
          <div class="submit-area">
            <van-button round block type="primary" native-type="submit" :loading="submitting">保存地址</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showAreaPicker" position="bottom" round>
      <van-area
        title="选择地区"
        :area-list="areaList"
        @confirm="onAreaConfirm"
        @cancel="showAreaPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';
import { useRoute, useRouter } from 'vue-router';
import { createUserAddress, deleteUserAddress, fetchUserAddresses, setDefaultAddress, updateUserAddress } from '../api';
import { areaTree } from '../utils/area';
import { formatAddress } from '../utils/format';

const SELECTED_ADDRESS_KEY = 'mall-h5-selected-address-id';

const route = useRoute();
const router = useRouter();
const addresses = ref([]);
const showPopup = ref(false);
const showAreaPicker = ref(false);
const submitting = ref(false);
const selectedAddressId = ref(localStorage.getItem(SELECTED_ADDRESS_KEY) || '');
const form = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  detailAddress: '',
  postalCode: '',
  isDefault: false,
});

const isSelectMode = computed(() => route.query.mode === 'select');
const returnPath = computed(() => route.query.from || '/checkout');

const areaList = {
  province_list: Object.fromEntries(areaTree.map((item) => [item.value, item.text])),
  city_list: Object.fromEntries(areaTree.flatMap((item) => item.children.map((city) => [city.value, city.text]))),
  county_list: Object.fromEntries(areaTree.flatMap((item) => item.children.flatMap((city) => city.children.map((county) => [county.value, county.text])))),
};

const regionText = computed(() => [form.provinceName, form.cityName, form.districtName].filter(Boolean).join(' / '));
const isValidMobile = (mobile) => /^1\d{10}$/.test(mobile || '');

const resetForm = () => {
  Object.assign(form, {
    id: null,
    receiverName: '',
    receiverPhone: '',
    provinceCode: '',
    provinceName: '',
    cityCode: '',
    cityName: '',
    districtCode: '',
    districtName: '',
    detailAddress: '',
    postalCode: '',
    isDefault: false,
  });
};

const normalizeAddress = (item) => ({
  ...item,
  default: Boolean(item?.default ?? item?.isDefault),
  isDefault: Boolean(item?.isDefault ?? item?.default),
});

const syncSelectedAddress = () => {
  if (!addresses.value.length) {
    selectedAddressId.value = '';
    localStorage.removeItem(SELECTED_ADDRESS_KEY);
    return;
  }

  const exists = addresses.value.some((item) => String(item.id) === String(selectedAddressId.value));
  if (exists) {
    localStorage.setItem(SELECTED_ADDRESS_KEY, String(selectedAddressId.value));
    return;
  }

  const fallback = addresses.value.find((item) => item.default) || addresses.value[0];
  selectedAddressId.value = fallback?.id != null ? String(fallback.id) : '';
  if (selectedAddressId.value) {
    localStorage.setItem(SELECTED_ADDRESS_KEY, String(selectedAddressId.value));
  }
};

const loadAddresses = async () => {
  const { data } = await fetchUserAddresses();
  addresses.value = (data.data || []).map(normalizeAddress);
  syncSelectedAddress();
};

const openCreate = () => {
  resetForm();
  showPopup.value = true;
};

const openEdit = (item) => {
  const normalizedItem = normalizeAddress(item);
  Object.assign(form, {
    id: normalizedItem.id,
    receiverName: normalizedItem.receiverName,
    receiverPhone: normalizedItem.receiverPhone,
    provinceCode: normalizedItem.provinceCode || '',
    provinceName: normalizedItem.provinceName || '',
    cityCode: normalizedItem.cityCode || '',
    cityName: normalizedItem.cityName || '',
    districtCode: normalizedItem.districtCode || '',
    districtName: normalizedItem.districtName || '',
    detailAddress: normalizedItem.detailAddress || '',
    postalCode: normalizedItem.postalCode || '',
    isDefault: normalizedItem.isDefault,
  });
  showPopup.value = true;
};

const onAreaConfirm = ({ selectedOptions }) => {
  const [province, city, county] = selectedOptions || [];
  form.provinceCode = province?.value || '';
  form.provinceName = province?.text || '';
  form.cityCode = city?.value || '';
  form.cityName = city?.text || '';
  form.districtCode = county?.value || '';
  form.districtName = county?.text || '';
  showAreaPicker.value = false;
};

const submitForm = async () => {
  if (!isValidMobile(form.receiverPhone)) {
    showFailToast('请输入正确的收货手机号');
    return;
  }
  if (!form.provinceName || !form.cityName || !form.districtName) {
    showFailToast('请选择完整的省市区');
    return;
  }
  try {
    submitting.value = true;
    const payload = {
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      provinceCode: form.provinceCode,
      provinceName: form.provinceName,
      cityCode: form.cityCode,
      cityName: form.cityName,
      districtCode: form.districtCode,
      districtName: form.districtName,
      detailAddress: form.detailAddress,
      postalCode: form.postalCode,
      isDefault: form.isDefault,
    };
    if (form.id) {
      await updateUserAddress(form.id, payload);
      showSuccessToast('地址更新成功');
    } else {
      await createUserAddress(payload);
      showSuccessToast('地址新增成功');
    }
    showPopup.value = false;
    await loadAddresses();
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '保存地址失败');
  } finally {
    submitting.value = false;
  }
};

const makeDefault = async (item) => {
  if (item.default) {
    return;
  }
  try {
    await setDefaultAddress(item.id);
    showSuccessToast('默认地址已更新');
    await loadAddresses();
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '设置默认地址失败');
  }
};

const removeAddress = async (item) => {
  try {
    await showConfirmDialog({ title: '确认删除', message: '删除后不可恢复，是否继续？' });
    await deleteUserAddress(item.id);
    showSuccessToast('删除成功');
    await loadAddresses();
  } catch (error) {
    if (error !== 'cancel') {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '删除地址失败');
    }
  }
};

const handleAddressClick = (item) => {
  if (!isSelectMode.value) {
    return;
  }
  selectedAddressId.value = String(item.id);
  localStorage.setItem(SELECTED_ADDRESS_KEY, String(item.id));
  showSuccessToast('收货地址已选择');
  router.replace(returnPath.value);
};

const handleBack = () => {
  if (isSelectMode.value) {
    router.replace(returnPath.value);
    return;
  }
  router.back();
};

loadAddresses().catch((error) => {
  showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '地址列表加载失败');
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 90px;
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.2), transparent 28%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.11), transparent 24%),
    linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
}

.list-wrap {
  padding: 12px;
}

.address-card {
  position: relative;
  margin-bottom: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.82);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
  transition: all 0.2s ease;
}

.selectable-card {
  cursor: pointer;
}

.default-card {
  border: 2px solid #60a5fa;
  background: #eff6ff;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.14);
}

.selected-card {
  border: 2px solid #1989fa;
  box-shadow: 0 10px 22px rgba(25, 137, 250, 0.18);
}

.default-card::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 14px;
  left: 0;
  width: 6px;
  border-radius: 0 999px 999px 0;
  background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%);
}

.address-head,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.address-head {
  justify-content: space-between;
}

.actions {
  margin-top: 16px;
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.actions :deep(.van-button) {
  min-width: 88px;
}

.contact-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.default-badge,
.selected-badge {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  border-radius: 999px;
}

.default-badge {
  color: #1d4ed8;
  background: #dbeafe;
}

.selected-badge {
  color: #0369a1;
  background: #e0f2fe;
}

.default-icon,
.selected-icon {
  font-size: 20px;
}

.default-icon {
  color: #2563eb;
}

.selected-icon {
  color: #1989fa;
}

.name {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.phone {
  color: #475569;
}

.detail {
  margin-top: 10px;
  color: #475569;
  line-height: 1.7;
}

.bottom-action {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(246, 248, 251, 0) 0%, #f6f8fb 35%, #f6f8fb 100%);
}

.popup-body {
  height: 100%;
  padding: 18px 0 24px;
  overflow-y: auto;
}

.popup-title {
  margin-bottom: 16px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
}

.submit-area {
  margin: 24px 16px 0;
}
</style>
