import type { AppRouteRecordRaw } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

export const AI_ROUTE: AppRouteRecordRaw = {
  path: '',
  name: 'ai-parent',
  component: LAYOUT,
  meta: {
    title: 'ai',
  },
  children: [
    {
      path: '/ai',
      name: 'ai',
      component: () => import('/@/views/dashboard/ai/index.vue'),
      meta: {
        title: 'AI助手',
      },
    },
  ],
};

export const AUTOCODE_PART_ROUTE: AppRouteRecordRaw = {
  path: '/autocodePart',
  name: 'autocodePart-parent',
  component: LAYOUT,
  meta: {
    title: '规则组成',
    hideBreadcrumb: true,
    hideMenu: true,
  },
  children: [
    {
      path: 'index/:id',
      name: 'autocodePart-index',
      component: () => import('/@/views/system/autocode/part/index.vue'),
      meta: {
        title: '规则组成',
        hideMenu: true,
        currentActiveMenu: '/system/autocode',
      },
    },
  ],
};

export const staticRoutesList = [AI_ROUTE, AUTOCODE_PART_ROUTE];
