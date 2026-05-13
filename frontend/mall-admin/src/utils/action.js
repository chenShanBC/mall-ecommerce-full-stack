import { ElMessageBox } from 'element-plus';

export async function confirmAction(message, title = '操作确认') {
  await ElMessageBox.confirm(message, title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
    draggable: true,
    closeOnClickModal: false,
    closeOnPressEscape: true,
    distinguishCancelAndClose: true,
    customClass: 'admin-confirm-box',
  });
}
