<template>
  <footer class="site-footer" aria-label="网站备案信息">
    <div class="site-footer__copyright">mall_fei商城 © 2026</div>
    <div class="site-footer__beian-row">
      <a
        class="site-footer__police-link"
        href="https://beian.mps.gov.cn/#/query/webSearch?code=44078302000388"
        target="_blank"
        rel="noopener noreferrer"
      >
        <svg class="site-footer__police-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 2.5 20 5.7v5.9c0 5-3.4 8.4-8 9.9-4.6-1.5-8-4.9-8-9.9V5.7l8-3.2Z" fill="#f5c542" />
          <path d="M12 4.8 17.8 7v4.5c0 3.4-2.2 5.9-5.8 7.2-3.6-1.3-5.8-3.8-5.8-7.2V7L12 4.8Z" fill="#d93131" />
          <path d="M12 7.1 14.9 12h-2l.7 3.4L9.1 10.5h2L12 7.1Z" fill="#ffe58a" />
          <path d="M8.3 7.9c.9.9 2.1 1.4 3.7 1.4s2.8-.5 3.7-1.4" fill="none" stroke="#ffe58a" stroke-width="1.2" stroke-linecap="round" />
        </svg>
        粤公网安备44078302000388号
      </a>
      <a
        class="site-footer__link"
        href="https://beian.miit.gov.cn/"
        target="_blank"
        rel="noopener noreferrer"
      >
        粤ICP备2026067922号-1
      </a>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  width: 100%;
  padding: 14px 24px 18px;
  color: #7d8796;
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

.site-footer__copyright {
  margin-bottom: 4px;
  color: #7d8796;
  font-size: 13px;
}

.site-footer__beian-row {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 3px 18px;
  max-width: 100%;
}

.site-footer__link,
.site-footer__police-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: inherit;
  text-decoration: none;
  transition: color 0.2s ease;
}

.site-footer__link:active,
.site-footer__link:hover,
.site-footer__police-link:active,
.site-footer__police-link:hover {
  color: #409eff;
}

.site-footer__police-icon {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin-right: 5px;
}

@media (max-width: 768px) {
  .site-footer {
    padding: 12px 16px 16px;
    font-size: 10px;
  }

  .site-footer__copyright {
    font-size: 10px;
  }

  .site-footer__beian-row {
    gap: 1px 10px;
  }

  .site-footer__police-icon {
    width: 15px;
    height: 15px;
  }
}
</style>
