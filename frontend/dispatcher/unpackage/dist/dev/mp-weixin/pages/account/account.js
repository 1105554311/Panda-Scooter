"use strict";
const common_vendor = require("../../common/vendor.js");
const api_modules_user = require("../../api/modules/user.js");
const DEFAULT_USER_INFO = {
  name: "访客调度员",
  email: "未登录"
};
const DEFAULT_DELETE_FORM = () => ({
  password: "",
  verificationCode: ""
});
const clearDispatcherSession = () => {
  common_vendor.index.removeStorageSync("dispatcherToken");
  common_vendor.index.removeStorageSync("dispatcherUserInfo");
  common_vendor.index.removeStorageSync("dispatcherCurrentTask");
};
const _sfc_main = {
  data() {
    return {
      userInfo: { ...DEFAULT_USER_INFO },
      deleteForm: DEFAULT_DELETE_FORM(),
      countdown: 0,
      timer: null,
      isSendingDeleteCode: false,
      isDeleting: false
    };
  },
  onShow() {
    const token = common_vendor.index.getStorageSync("dispatcherToken");
    if (!token) {
      common_vendor.index.redirectTo({
        url: "/pages/login/login?mode=login"
      });
      return;
    }
    this.loadUserInfo();
  },
  onUnload() {
    this.clearTimer();
  },
  methods: {
    async loadUserInfo() {
      try {
        const res = await api_modules_user.getDispatcherInfo();
        const data = res.data || {};
        this.userInfo = {
          name: data.name || DEFAULT_USER_INFO.name,
          email: data.email || DEFAULT_USER_INFO.email
        };
      } catch (error) {
        const cached = common_vendor.index.getStorageSync("dispatcherUserInfo") || {};
        this.userInfo = {
          ...DEFAULT_USER_INFO,
          ...cached
        };
      }
    },
    navigateToResetPassword() {
      const email = encodeURIComponent(this.userInfo.email || "");
      common_vendor.index.navigateTo({
        url: `/pages/resetPassword/resetPassword${email ? `?email=${email}` : ""}`
      });
    },
    async sendDeleteCode() {
      if (!this.userInfo.email || this.userInfo.email === DEFAULT_USER_INFO.email) {
        common_vendor.index.showToast({
          title: "当前账号暂无可用邮箱",
          icon: "none"
        });
        return;
      }
      if (this.isSendingDeleteCode) {
        return;
      }
      this.isSendingDeleteCode = true;
      try {
        await api_modules_user.getVerificationCode(this.userInfo.email);
        common_vendor.index.showToast({
          title: "验证码已发送",
          icon: "success"
        });
        this.startCountdown();
      } catch (error) {
      } finally {
        this.isSendingDeleteCode = false;
      }
    },
    startCountdown() {
      this.clearTimer();
      this.countdown = 60;
      this.timer = setInterval(() => {
        if (this.countdown <= 1) {
          this.clearTimer();
          this.countdown = 0;
          return;
        }
        this.countdown -= 1;
      }, 1e3);
    },
    clearTimer() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },
    async logout() {
      common_vendor.index.showModal({
        title: "退出登录",
        content: "确认退出当前调度员账号吗？",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          try {
            await api_modules_user.dispatcherLogout();
          } catch (error) {
          }
          clearDispatcherSession();
          common_vendor.index.showToast({
            title: "已退出登录",
            icon: "success"
          });
          setTimeout(() => {
            common_vendor.index.reLaunch({
              url: "/pages/login/login?mode=login"
            });
          }, 800);
        }
      });
    },
    async deleteAccount() {
      if (!this.deleteForm.password || !this.deleteForm.verificationCode) {
        common_vendor.index.showToast({
          title: "请填写完整信息",
          icon: "none"
        });
        return;
      }
      if (this.isDeleting) {
        return;
      }
      common_vendor.index.showModal({
        title: "注销账号",
        content: "注销后将删除当前账号并退出登录，确认继续吗？",
        confirmColor: "#ff4d4f",
        success: async (res) => {
          if (!res.confirm) {
            return;
          }
          this.isDeleting = true;
          try {
            common_vendor.index.showLoading({
              title: "注销中..."
            });
            await api_modules_user.dispatcherDelete({
              password: this.deleteForm.password,
              verificationCode: this.deleteForm.verificationCode
            });
            common_vendor.index.hideLoading();
            clearDispatcherSession();
            common_vendor.index.showToast({
              title: "账号已注销",
              icon: "success"
            });
            setTimeout(() => {
              common_vendor.index.reLaunch({
                url: "/pages/login/login?mode=login"
              });
            }, 800);
          } catch (error) {
            common_vendor.index.hideLoading();
          } finally {
            this.isDeleting = false;
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.t($data.userInfo.name),
    b: common_vendor.t($data.userInfo.email),
    c: common_vendor.o((...args) => $options.navigateToResetPassword && $options.navigateToResetPassword(...args), "c5"),
    d: common_vendor.o((...args) => $options.logout && $options.logout(...args), "0a"),
    e: $data.deleteForm.password,
    f: common_vendor.o(common_vendor.m(($event) => $data.deleteForm.password = $event.detail.value, {
      trim: true
    }), "7f"),
    g: $data.deleteForm.verificationCode,
    h: common_vendor.o(common_vendor.m(($event) => $data.deleteForm.verificationCode = $event.detail.value, {
      trim: true
    }), "bb"),
    i: common_vendor.t($data.isSendingDeleteCode ? "发送中..." : $data.countdown > 0 ? `${$data.countdown}s` : "获取验证码"),
    j: $data.countdown > 0 || $data.isSendingDeleteCode,
    k: common_vendor.o((...args) => $options.sendDeleteCode && $options.sendDeleteCode(...args), "52"),
    l: common_vendor.t($data.isDeleting ? "注销中..." : "确认注销"),
    m: $data.isDeleting,
    n: common_vendor.o((...args) => $options.deleteAccount && $options.deleteAccount(...args), "06")
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../.sourcemap/mp-weixin/pages/account/account.js.map
