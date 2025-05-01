<!-- 头部公用 -->
<template>
  <div class="">
    <div class="headBack">
      <el-row class="container">
        <el-col :span="24">
          <!-- pc端导航 -->
          <div class="headBox">
            <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect" :router="true">
              <el-menu-item index="/Home" @click="goToHome"><i class="fa fa-wa fa-home"></i> 首页</el-menu-item>
              <el-submenu index="/Share">
                <template slot="title"><i class="fa fa-wa fa-archive"></i> 分类</template>
                <el-menu-item v-for="(item,index) in classListObj" :key="'class1'+index" :index="'/Share?classId='+item.id">{{item.name}}</el-menu-item>
              </el-submenu>
              <el-menu-item index="/Friendslink"><i class="fa fa-wa fa-users"></i>友链</el-menu-item>
              <el-menu-item index="/Chat"><i class="fa fa-wa"> 私信</i></el-menu-item>
              
              <!-- 通知菜单改为下拉菜单 -->
              <li class="notification-dropdown" v-if="haslogin" @click.stop="toggleNotificationsMenu">
                <a href="javascript:void(0);" class="notification-icon">
                  <i class="fa fa-bell"></i>
                  <el-badge :value="unreadCount" class="notification-badge" v-if="unreadCount > 0"></el-badge>
                </a>
                <!-- 下拉通知菜单 -->
                <div class="notifications-dropdown-menu" v-show="showNotificationsMenu" @click.stop>
                  <div class="notifications-header">
                    <h3>通知中心</h3>
                    <el-button
                      type="text"
                      size="small"
                      @click="markAllAsRead"
                      :disabled="notifications.length === 0 || unreadCount === 0">
                      全部标记为已读
                      <span v-if="unreadCount > 0" class="unread-badge-mini">{{unreadCount}}</span>
                    </el-button>
                  </div>
                  
                  <div class="notifications-body">
                    <!-- 显示加载动画 -->
                    <div v-if="loading" class="loading-container">
                      <i class="el-icon-loading"></i>
                      <p>正在加载通知...</p>
                    </div>
                    
                    <!-- 通知列表 -->
                    <div v-else-if="notifications.length > 0" class="notifications-list">
                      <div v-for="(notification, index) in notifications"
                          :key="notification.notificationId"
                          class="notification-item"
                          :class="{'unread': !notification.read}">
                        <div class="notification-content">
                          <div class="avatar" @click="goToArticle(notification.articleId)">
                            <img :src="notification.fromUserAvatar || 'static/img/default-avatar.png'" alt="avatar">
                            <div class="status-indicator" v-if="!notification.read"></div>
                          </div>
                          <div class="details">
                            <p class="user-info">
                              <strong>{{ notification.fromUserNickName }}</strong>
                              <span class="action-text" v-if="notification.commentType === '0'">评论了你的文章</span>
                              <span class="action-text" v-else>回复了你的评论</span>
                            </p>
                            <p class="time"><i class="el-icon-time"></i> {{ formatTime(notification.createTime) }}</p>
                            <el-button 
                              class="view-article-btn" 
                              size="mini" 
                              type="primary" 
                              plain
                              @click="goToArticle(notification.articleId)">
                              查看文章
                            </el-button>
                          </div>
                          <div class="actions">
                            <el-button
                              type="text"
                              class="mark-read-btn"
                              :class="{'disabled': notification.read}"
                              @click="markAsRead(notification.notificationId)"
                              :disabled="notification.read">
                              <i class="el-icon-check"></i>
                            </el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    <div v-else class="no-notifications">
                      <i class="el-icon-bell"></i>
                      <p>暂无通知</p>
                    </div>
                  </div>
                </div>
              </li>
              
              <div class="userInfo">
                <div v-show="!haslogin" class="nologin">
                  <a href="javascript:void(0);" @click="logoinFun(1)">登录&nbsp;</a>|<a href="javascript:void(0);" @click="logoinFun(0)">&nbsp;注册</a>
                </div>
                <div v-show="haslogin" class="haslogin">
                  <i class="fa fa-fw fa-user-circle userImg"></i>
                  <ul class="haslogin-info">
                    <li>
                      <a href="#/UserInfo">个人中心</a>
                    </li>
                    <li>
                      <a href="javascript:void(0);" @click="userlogout">退出登录</a>
                    </li>
                  </ul>
                </div>
              </div>
            </el-menu>
          </div>
        </el-col>
      </el-row>
    </div>
    <div class="headImgBox" :style="{backgroundImage:this.$store.state.themeObj.top_image?'url('+this.$store.state.themeObj.top_image+')':'url(static/img/headbg05.png)'}">
      <div class="scene">
        <div><span id="luke"></span></div>
      </div>
      <div class="h-information">
        <img :src="this.$store.state.themeObj.head_portrait?this.$store.state.themeObj.head_portrait:'static/img/tou.png'" alt="">
        <h2 class="h-description">
          {{this.$store.state.themeObj.autograph?this.$store.state.themeObj.autograph:"岁月为我大浪淘沙，你却被留下"}}
        </h2>
      </div>
    </div>
  </div>
</template>

<script>
import {logout} from '../api/user'
import {removeToken} from '../utils/auth'
import {getCategoryList} from '../api/category'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead
} from '../api/notification'
import {Typeit} from '../utils/plug.js'

export default {
  data() {
    return {
      userInfo: '', //用户信息
      haslogin: false, //是否已登录
      classListObj: '', //分类
      activeIndex: '/', //当前选择的路由模块
      state: '', //icon点击状态
      pMenu: true, //手机端菜单打开
      input: '', //input输入内容
      headBg: 'url(static/img/headbg05.jpg)', //头部背景图
      headTou: '', //头像
      projectList: '', //项目列表
      unreadCount: 0, // 未读通知数量
      showNotificationsMenu: false, // 控制通知下拉菜单的显示
      notifications: [], // 通知列表
      loading: false, // 加载状态
      pageNum: 1, // 页码
      pageSize: 5, // 每页显示条数
      processing: false, // 处理状态标记
    }
  },
  watch: {
    // 如果路由有变化，会再次执行该方法
    '$route': 'routeChange',
    haslogin(newVal) {
      if (newVal) {
        // 登录后获取一次通知数量，但不开启轮询
        this.fetchUnreadCount();
      } else {
        this.unreadCount = 0;
        this.notifications = [];
      }
    },
    // 显示通知菜单时加载通知列表
    showNotificationsMenu(newVal) {
      if (newVal && this.haslogin) {
        // 当打开通知菜单时，同时获取最新的通知列表和未读数量
        this.fetchNotifications();
        this.fetchUnreadCount();
      }
    }
  },
  methods: {
    // 格式化时间
    formatTime(timestamp) {
      const date = new Date(Number(timestamp));
      return date.toLocaleString();
    },
    handleOpen(key, keyPath) {
      // 分组菜单打开
    },
    handleClose(key, keyPath) {
      // 分组菜单关闭
    },
    searchChangeFun(e) {
      if (this.input == '') {
        this.$store.state.keywords = '';
        this.$router.push({path:'/'});
      }
    },
    getCategoryList() {
      getCategoryList().then((response) => {
        this.classListObj = response
      })
    },
    handleSelect(key, keyPath) {
      // pc菜单选择
      console.log('菜单选择:', key, keyPath);
    },
    // 跳转到首页
    goToHome() {
      console.log('跳转到首页');
      if (this.$route.path !== '/') {
        this.$router.push({
          path: '/'
        });
      } else {
        // 如果已经在首页，可以考虑刷新内容或者回到顶部等操作
        window.scrollTo(0, 0);
      }
    },
    logoinFun: function(msg) {
      localStorage.setItem('logUrl', this.$route.fullPath);
      if (msg == 0) {
        this.$router.push({
          path: '/Login?login=0'
        });
      } else {
        this.$router.push({
          path: '/Login?login=1'
        });
      }
    },
    userlogout: function() {
      var that = this;
      this.$confirm('是否确认退出?', '退出提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        logout().then((response) => {
          removeToken()
          localStorage.removeItem('userInfo');
          that.haslogin = false;
          that.unreadCount = 0; // 退出登录时清空未读数量
          that.notifications = []; // 清空通知列表
          window.location.reload();
          that.$message({
            type: 'success',
            message: '退出成功!'
          });
          if (that.$route.path == '/UserInfo') {
            that.$router.push({
              path: '/'
            });
          }
        })
      }).catch(() => {
        //
      });
    },
    // 获取通知列表
    fetchNotifications() {
      // 检查用户信息
      if (!this.userInfo || !this.userInfo.id) {
        console.error('用户未登录或用户信息无效');
        return;
      }

      this.loading = true; // 开始加载
      getNotifications(this.userInfo.id, this.pageNum, this.pageSize)
        .then(response => {
          if (response && response.rows) {
            this.notifications = response.rows;
            this.unreadCount = this.notifications.filter(n => !n.read).length;
          } else {
            this.notifications = [];
            // 不在界面上显示错误消息，只在控制台记录
            console.warn('响应数据格式不符合预期:', response);
          }
        })
        .catch(error => {
          console.error('获取通知失败:', error);
          this.notifications = [];
          // 不显示错误消息提示，避免干扰用户体验
        })
        .finally(() => {
          this.loading = false; // 加载完成
        });
    },
    // 获取未读通知数量
    fetchUnreadCount() {
      if (this.haslogin && this.userInfo && this.userInfo.id) {
        getUnreadNotificationCount(this.userInfo.id)
          .then(response => {
            // 更新未读通知数量
            this.unreadCount = response.data || 0;
          })
          .catch(error => {
            console.error('获取未读通知数量失败:', error);
            // 出错时不更改现有未读数量，保持当前状态
          });
      }
    },
    // 切换通知菜单的显示状态
    toggleNotificationsMenu() {
      this.showNotificationsMenu = !this.showNotificationsMenu;
      
      // 当打开通知菜单时，获取最新的通知
      if (this.showNotificationsMenu && this.haslogin) {
        this.fetchUnreadCount();
        this.fetchNotifications();
      }
      
      // 点击其他地方关闭菜单
      if (this.showNotificationsMenu) {
        setTimeout(() => {
          document.addEventListener('click', this.closeNotificationsMenu);
        }, 0);
      }
    },
    // 关闭通知菜单
    closeNotificationsMenu() {
      this.showNotificationsMenu = false;
      document.removeEventListener('click', this.closeNotificationsMenu);
    },
    // 跳转到文章页面
    goToArticle(articleId) {
      if (articleId) {
        this.showNotificationsMenu = false; // 关闭菜单
        this.$router.push({ 
          path: '/DetailArticle', 
          query: { aid: articleId }
        });
      }
    },
    // 标记单条通知为已读
    markAsRead(notificationId) {
      // 如果通知已经被标记为已读，则不发送请求
      const notification = this.notifications.find(n => n.notificationId === notificationId);
      if (!notification || notification.read || notification.processing) {
        return;
      }
      
      notification.processing = true;
      
      markNotificationAsRead(this.userInfo.id, notificationId)
        .then(response => {
          // 标记成功后，从列表中移除该通知
          this.notifications = this.notifications.filter(n => n.notificationId !== notificationId);
          this.unreadCount = Math.max(0, this.unreadCount - 1);
          
          this.$message({
            message: '已标记为已读',
            type: 'success',
            duration: 1500 // 缩短提示时间
          });
        })
        .catch(error => {
          console.error('标记通知为已读失败:', error);
          this.$message.error('操作失败，请稍后重试');
        })
        .finally(() => {
          // 不管成功或失败，都取消处理中状态
          if (notification) {
            notification.processing = false;
          }
        });
    },
    // 标记全部通知为已读   
    markAllAsRead() {
      // 如果没有未读通知，则不发送请求
      if (this.unreadCount <= 0 || this.processing) {
        return;
      }
      
      // 设置处理中状态，防止重复点击
      this.processing = true;
      
      markAllNotificationsAsRead(this.userInfo.id)
        .then(response => {
          // 标记全部已读后，清空通知列表
          this.notifications = [];
          this.unreadCount = 0;
          
          this.$message({
            message: '已全部标记为已读',
            type: 'success',
            duration: 2000
          });
        })
        .catch(error => {
          console.error('标记全部通知为已读失败:', error);
          this.$message.error('标记全部已读失败，请稍后重试');
        })
        .finally(() => {
          // 不管成功或失败，都取消处理中状态
          this.processing = false;
        });
    },
    routeChange: function() {
      var that = this;
      that.pMenu = true
      // 如果当前路径是根路径，设置activeIndex为/Home
      this.activeIndex = this.$route.path === '/' ? '/Home' : this.$route.path;
      if (localStorage.getItem('userInfo')) {
        that.haslogin = true;
        that.userInfo = JSON.parse(localStorage.getItem('userInfo'));
        this.fetchUnreadCount(); // 在路由变化时重新获取未读数量
      } else {
        that.haslogin = false;
        that.unreadCount = 0;
        that.notifications = [];
      }
      //获取分类
      this.getCategoryList()

      if ((this.$route.name == "Share" || this.$route.name == "Home") && this.$store.state.keywords) {
        this.input = this.$store.state.keywords;
      } else {
        this.input = '';
        this.$store.state.keywords = '';
      }
    }
  },
  components: {
    // 定义组件
  },
  created() {
    //判断当前页面是否被隐藏
    var that = this;
    var hiddenProperty = 'hidden' in document ? 'hidden' :
      'webkitHidden' in document ? 'webkitHidden' :
        'mozHidden' in document ? 'mozHidden' :
          null;
    var visibilityChangeEvent = hiddenProperty.replace(/hidden/i, 'visibilitychange');
    var onVisibilityChange = function() {
      if (!document[hiddenProperty]) {
        // 页面变为可见时
        if (that.$route.path != '/DetailShare') {
          if (localStorage.getItem('userInfo')) {
            that.haslogin = true;
            // 页面变为可见时，如果通知菜单正在显示，才刷新通知
            if (that.showNotificationsMenu) {
              that.fetchUnreadCount();
              that.fetchNotifications();
            }
          } else {
            that.haslogin = false;
            that.unreadCount = 0;
            that.notifications = [];
          }
        }
      }
    }
    document.addEventListener(visibilityChangeEvent, onVisibilityChange);
    this.routeChange();
    
    // 页面加载时检查用户登录状态并初始化通知
    if (localStorage.getItem('userInfo')) {
      this.userInfo = JSON.parse(localStorage.getItem('userInfo'));
      this.haslogin = true;
      // 初始化时获取一次通知数量
      this.$nextTick(() => {
        this.fetchUnreadCount();
      });
    }
  },
  mounted() {
    var that = this;
    var timer = setTimeout(function() {
      Typeit(that.$store.state.themeObj.user_start, "#luke");
      clearTimeout(timer);
    }, 500);
    
    // 登录后初始获取一次通知数量，但不设置轮询
    if (this.haslogin) {
      this.fetchUnreadCount();
    }
  },
  beforeDestroy() {
    // 组件销毁前移除监听器
    document.removeEventListener('click', this.closeNotificationsMenu);
  }
}
</script>

<style>
/*********头部导航栏********/

/*头部导航栏盒子*/

.headBack {
	width: 100%;
	background: rgba(40, 42, 44, 0.6);
	/*margin-bottom:30px;*/
	box-shadow: 0 2px 4px 0 rgba(0, 0, 0, .12), 0 0 6px 0 rgba(0, 0, 0, .04);
	position: fixed;
	left: 0;
	top: 0;
	right: 0;
	z-index: 100;
}

.headBox li.is-active {
	/*background: #48456C;*/
	background: rgba(73, 69, 107, 0.7);
}

.el-menu--horizontal>.el-submenu.is-active .el-submenu__title {
	border-bottom: none!important;
}

.headBox .el-menu {
	background: transparent;
	border-bottom: none!important;
}

.headBox .el-menu-demo li.el-menu-item,
.headBox .el-menu--horizontal .el-submenu .el-submenu__title {
	height: 38px;
	line-height: 38px;
	border-bottom: none!important;

}

.headBox .el-submenu li.el-menu-item {
	height: 38px;
	line-height: 38px;
}

.headBox li .fa-wa {
	vertical-align: baseline;
}

.headBox ul li.el-menu-item,
.headBox ul li.el-menu-item.is-active,
.headBox ul li.el-menu-item:hover,
.headBox .el-submenu div.el-submenu__title,
.headBox .el-submenu__title i.el-submenu__icon-arrow {
	color: #fff;
}

.headBox .el-menu--horizontal .el-submenu>.el-menu {
	top: 38px;
	border: none;
	padding: 0;
}

.headBox>ul li.el-menu-item:hover,
.headBox>ul li.el-submenu:hover .el-submenu__title {
	background: #48456C;
	border-bottom: none;
}

.headBox>ul .el-submenu .el-menu,
.headBox>ul .el-submenu .el-menu .el-menu-item {
	background: #48456C;
}

.headBox>ul .el-submenu .el-menu .el-menu-item {
	min-width: 0;
}

.headBox>ul .el-submenu .el-menu .el-menu-item:hover {
	background: #64609E;
}

/*pc搜索框*/

.headBox .pcsearchbox {
	padding: 0;
	max-width: 170px;
	/*min-width: 30px;*/
	height: 100%;
	line-height: 38px;
	position: absolute;
	top: 0;
	right: 0;
	cursor: pointer;
}

.headBox .pcsearchbox:hover .pcsearchinput {
	opacity: 1;
	/*transform: scaleX(1);*/
	visibility: visible;
}

.headBox .pcsearchbox i.pcsearchicon {
	color: #fff;
	padding-left: 10px;
}

.headBox .pcsearchbox .pcsearchinput {
	width: 180px;
	padding: 10px 20px 10px 20px;
	background: rgba(40, 42, 44, 0.6);
	border-radius: 0 0 2px 2px;
	position: absolute;
	right: 0;
	top: 38px;
	opacity: 0;
	visibility: hidden;
	/*transform: scaleX(0);*/
	transform-origin: right;
	transition: all 0.3s ease-out;
}

.headBox .pcsearchbox .hasSearched {
	opacity: 1;
	/*transform: scaleX(1);*/
	visibility: visible;
}

.headBox .pcsearchbox .el-input {
	width: 100%;
}

.headBox .el-input__inner {
	height: 30px;
	border: none;
	background: #fff;
	/*border: 1px solid #333;*/
	border-radius: 2px;
	padding-right: 10px;
}

.headBox .userInfo {
	height: 100%;
	line-height: 38px;
	position: absolute;
	right: 30px;
	top: 0;
	color: #fff;
}

.headBox .userInfo a {
	color: #fff;
	font-size: 13px;
	transition: all 0.2s ease-out;
}

.headBox .userInfo a:hover {
	color: #48456C;
}

.headBox .nologin {
	text-align: right;
}

.headBox .haslogin {
	text-align: right;
	position: relative;
	min-width: 80px;
	cursor: pointer;
}

.headBox .haslogin:hover ul {
	visibility: visible;
	opacity: 1;
}

.headBox .haslogin ul {
	background: rgba(40, 42, 44, 0.6);
	padding: 5px 10px;
	position: absolute;
	right: 0;
	visibility: hidden;
	opacity: 0;
	transition: all 0.3s ease-out;
}

.headBox .haslogin ul li {
	border-bottom: 1px solid #48456C;
}

.headBox .haslogin ul li:last-child {
	border-bottom: 1px solid transparent;
}

/*******移动端*******/

.mobileBox {
	position: relative;
	height: 38px;
	line-height: 38px;
	color: #fff;
}

.hideMenu {
	position: relative;
	width: 100%;
	height: 100%;
	line-height: 38px;
}

.hideMenu ul.mlistmenu {
	width: 100%;
	position: absolute;
	left: 0;
	top: 100%;
	box-sizing: border-box;
	z-index: 999;
	box-shadow: 0 2px 6px 0 rgba(0, 0, 0, .12), 0 0 8px 0 rgba(0, 0, 0, .04);
	background: #48456C;
	color: #fff;
	border-right: none;
}

.hideMenu .el-submenu .el-menu {
	background: #64609E;
}

.hideMenu .el-menu-item,
.hideMenu .el-submenu__title {
	color: #fff;
}

.hideMenu>i {
	position: absolute;
	left: 10px;
	top: 12px;
	width: 30px;
	height: 30px;
	font-size: 16px;
	color: #fff;
	cursor: pointer;
}

.hideMenu .el-menu-item,
.el-submenu__title {
	height: 40px;
	line-height: 40px;
}

.mobileBox .searchBox {
	padding-left: 40px;
	width: 100%;
	box-sizing: border-box;
}

.mobileBox .searchBox .el-input__inner {
	display: block;
	border-radius: 2px;
	border: none;
	height: 25px;
}

.hideMenu ul.mlistmenu.pshow {
	display: block;
}

.hideMenu ul.mlistmenu .el-submenu__icon-arrow,
.mobileBox li.el-menu-item a {
	color: #fff;
}

.hideMenu>ul li.el-menu-item:hover,
.hideMenu>ul li.el-menu-item.is-active {
	background: #48576a;
}



/*头部背景图*/

.headImgBox {
	height: 650px;
	position: relative;
	width: 100%;
	background-size: cover;
	background-position: center 50%;
	background-repeat: no-repeat;
	margin-bottom: 90px;
}

.h-information {
	text-align: center;
	width: 70%;
	margin: auto;
	position: relative;
	top: 480px;
	padding: 40px 0;
	font-size: 16px;
	opacity: 0.98;
	background: rgba(230, 244, 249, 0.8);
	border-radius: 5px;
	z-index: 1;
	animation: b 1s ease-out;
	-webkit-animation: b 1s ease-out;
}

@-webkit-keyframes b {
	0% {
		-webkit-transform: translateY(90px);
		transform: translateY(90px);
	}
	80% {
		-webkit-transform: translateY(5px);
		transform: translateY(5px)
	}
	90% {
		-webkit-transform: translateY(-5px);
		transform: translateY(-5px)
	}
	to {
		-webkit-transform: translateY(0);
		transform: translateY(0)
	}
}

@keyframes b {
	0% {
		-webkit-transform: translateY(90px);
		transform: translateY(90px);
	}
	80% {
		-webkit-transform: translateY(5px);
		transform: translateY(5px)
	}
	90% {
		-webkit-transform: translateY(-5px);
		transform: translateY(-5px)
	}
	to {
		-webkit-transform: translateY(0);
		transform: translateY(0)
	}
}

.h-information img {
	width: 100px;
	height: 100px;
	border-radius: 100%;
	transition: all .4s ease-in-out;
	-webkit-transition: all .4s ease-in-out;
	object-fit: cover;
}

.h-information img:hover {
	transform: rotate(360deg);
	-webkit-transform: rotate(360deg);
}

.h-information h2 {
	margin-top: 20px;
	font-size: 18px;
	font-weight: 700;
	/*font-family: 'Sigmar One';*/
}
.h-information h2  a{
	background: linear-gradient(to right, #DF2050, #48456D);
	-webkit-background-clip: text;
	color: transparent;
}
.headImgBox .scene {
	width: 100%;
	/*height:300px;*/
	text-align: center;
	font-size: 100px;
	font-weight: 200;
	color: #fff;
	position: absolute;
	left: 0;
	top: 160px;
	font-family: 'Sigmar One', Arial;
	text-shadow: 0 2px 2px #47456d;

}

.headImgBox .scene span {
	transform: matrix(1, 0, 0, 1, 0, 0);
	-webkit-transform: matrix(1, 0, 0, 1, 0, 0);
	text-shadow: 1px 1px 0 #ff3f1a, -1px -1px 0 #00a7e0;
}

.saying:after {
	content: "|";
	font-family: Arial, sans-serif;
	font-size: 1em;
	/*line-height: 0;*/
	display: inline-block;
	vertical-align: baseline;
	opacity: 1;
	text-shadow: 1px 1px 0 #ff3f1a, -1px -1px 0 #00a7e0;
	animation: caret 500ms infinite;
}

@keyframes caret {
	0%,
	100% {
		opacity: 1;
	}
	50% {
		opacity: 0;
	}
}

/* 通知下拉菜单样式 */
.notification-dropdown {
  position: relative;
  display: inline-block;
  height: 38px;
  line-height: 38px;
  margin: 0;
  padding: 0 20px;
  cursor: pointer;
  color: #fff;
}

.notification-dropdown:hover {
  background: #48456C;
}

.notification-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  position: relative;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
}

.notification-badge.el-badge >>> .el-badge__content {
  background-color: #f56c6c;
  border: none;
  font-size: 12px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  color: #fff;
  display: inline-block !important;
  box-shadow: 0 0 0 1px #fff;
}

.notifications-dropdown-menu {
  position: absolute;
  right: 0;
  top: 38px;
  background: #fff;
  min-width: 350px;
  max-width: 400px;
  max-height: 500px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
}

.notifications-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
  border-radius: 4px 4px 0 0;
}

.notifications-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.notifications-header h3:before {
  content: "\f0a2";  /* FontAwesome 铃铛图标编码 */
  font-family: FontAwesome;
  margin-right: 5px;
  color: #409EFF;
}

.unread-badge-mini {
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  margin-left: 5px;
}

.notifications-body {
  padding: 0;
  max-height: 400px;
  overflow-y: auto;
}

.notifications-list {
  padding: 0;
}

.notification-item {
  padding: 12px 15px;
  border-bottom: 1px solid #ebeef5;
  transition: all 0.3s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #f0f8ff;
  border-left: 3px solid #409EFF;
}

.notification-content {
  display: flex;
  align-items: flex-start;
}

.avatar {
  position: relative;
  margin-right: 10px;
  cursor: pointer;
}

.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.status-indicator {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  border: 1px solid #fff;
}

.details {
  flex: 1;
  min-width: 0;
}

.user-info {
  margin: 0 0 5px;
  font-size: 14px;
  line-height: 1.4;
  color: #303133;
  word-break: break-word;
}

.user-info strong {
  color: #409EFF;
}

.time {
  font-size: 12px;
  color: #909399;
  margin: 0 0 5px;
}

.view-article-btn {
  padding: 3px 8px;
  font-size: 12px;
}

.actions {
  margin-left: 10px;
}

.mark-read-btn {
  padding: 5px;
  color: #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
  border-radius: 50%;
}

.mark-read-btn:hover {
  color: #fff;
  background-color: #67c23a;
}

.mark-read-btn.disabled {
  color: #c0c4cc;
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.no-notifications {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.no-notifications i {
  font-size: 30px;
  color: #dcdfe6;
  margin-bottom: 10px;
}

.loading-container {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.loading-container i {
  animation: rotating 1s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 通知图标闪烁动画 */
@keyframes notification-blink {
  0% { transform: scale(1); color: #fff; }
  50% { transform: scale(1.2); color: #f56c6c; }
  100% { transform: scale(1); color: #fff; }
}

.notification-blink {
  animation: notification-blink 0.5s ease-in-out 3;
}

/* 通知图标样式强化 */
.notification-icon i {
  font-size: 18px;
  vertical-align: middle;
}

/* 移动设备上的通知样式 */
@media screen and (max-width: 768px) {
  .notifications-dropdown-menu {
    position: fixed;
    top: 38px;
    left: 0;
    right: 0;
    width: 100%;
    max-width: 100%;
    border-radius: 0;
    max-height: 80vh;
  }
  
  .notification-dropdown {
    padding: 0 10px;
  }
  
  .notifications-body {
    max-height: 60vh;
  }
  
  .notification-item {
    padding: 10px;
  }
  
  .avatar img {
    width: 35px;
    height: 35px;
  }
  
  .details {
    font-size: 13px;
  }
  
  .time {
    font-size: 11px;
  }
}

/* 确保通知图标在各种状态下都能正确显示 */
.notification-icon {
  transition: all 0.3s ease;
}

.notification-icon:hover {
  transform: scale(1.1);
}

/* 强制确保通知徽章显示 */
.el-badge__content {
  display: inline-block !important;
  visibility: visible !important;
  opacity: 1 !important;
}
</style>
