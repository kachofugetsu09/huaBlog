
<!-- 文章详情 -->
<template>
  <div>
    <sg-nav></sg-nav>
    <div class="container" id="detail">
      <el-row :gutter="30">
        <el-col :sm="24" :md="24" style="transition:all .5s ease-out;margin-bottom:30px;">
          <sg-articleDetail class="shadow-effect"></sg-articleDetail>
          <sg-message class="shadow-effect" @avatar-click="handleAvatarClick"></sg-message>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
<script>
import header from '../components/header.vue'
import articleDetail from '../components/articleDetail.vue'
import message from '../components/message.vue'
import { getChatSessions, createSession } from "../api/chat"
import { getToken } from '../utils/auth'

export default {
  name:'DetailShare',
  data() { //选项 / 数据
    return {
      loading: false
    }
  },

  methods: {
    async handleAvatarClick(targetUserId) {
      if (!targetUserId) {
        this.$message.error("目标用户ID未定义");
        return;
      }

      try {
        // Check if user is logged in
        const token = getToken();
        if (!token) {
          this.$confirm('需要登录才能发起聊天，是否前往登录页面?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            localStorage.setItem('logUrl', this.$route.fullPath);
            this.$router.push({path:'/Login?login=1'});
          });
          return;
        }

        // Get current user info
        const userInfo = JSON.parse(localStorage.getItem('userInfo'));
        if (!userInfo || !userInfo.id) {
          this.$message.error("用户信息不完整，请重新登录");
          return;
        }

        // Check if user is trying to chat with themselves
        if (userInfo.id == targetUserId) {
          this.$message.warning("不能与自己发起聊天");
          return;
        }

        this.loading = true;

        // Create or get existing session
        const response = await createSession(userInfo.id, targetUserId);
        const sessionId = response.data;

        // Navigate to chat with the session ID
        this.$router.push(`/Chat?sessionId=${sessionId}&targetId=${targetUserId}`);
      } catch (error) {
        console.error("处理头像点击时出错:", error);
        if (error.response && error.response.data && error.response.data.message) {
          this.$message.error(error.response.data.message);
        } else {
          this.$message.error("无法启动聊天，请稍后再试");
        }
      } finally {
        this.loading = false;
      }
    }
  },
  components: { //定义组件
    'sg-nav':header,
    'sg-articleDetail':articleDetail,
    'sg-message':message,
    //'sg-rightlist':rightlist,
  },
  created() { //生命周期函数
  },
  beforeDestroy() {
    // Clean up event listener
  },
  mounted(){
    var anchor = document.querySelector("#detail");
    // console.log(anchor,anchor.offsetTop);
    var top = anchor.offsetTop-60;
    document.body.scrollTop = top;
    // Firefox
    document.documentElement.scrollTop = top;
    // Safari
    window.pageYOffset = top;
  }
}
</script>

<style scoped>
/* 阴影效果样式 */
.shadow-effect {
  background-color: #fff; /* 背景颜色可以根据需要调整 */
  padding: 20px; /* 内边距让内容与边框有一定距离 */
  border-radius: 8px; /* 圆角效果 */
  box-shadow:
    -15px -15px 30px rgba(255, 255, 255, 0.8), /* 左上角高光，更亮更明显 */
    15px 15px 30px rgba(0, 0, 0, 0.2); /* 右下角阴影，更深更明显 */
  transition: all 0.3s ease; /* 添加平滑过渡效果 */
}

/* 鼠标悬停时的阴影增强效果 */
.shadow-effect:hover {
  box-shadow:
    -20px -20px 40px rgba(255, 255, 255, 0.9), /* 更亮的左上角高光 */
    20px 20px 40px rgba(0, 0, 0, 0.3); /* 更深的右下角阴影 */
}
</style>
