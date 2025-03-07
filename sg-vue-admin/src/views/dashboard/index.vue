<template>
  <div class="dashboard-container" id="#login-img">
    <div class="dashboard-text" style="font-weight: bolder">
      个人博客管理系统
    </div>
    <div>
      基于SpringBoot + MyBatis-Plus + SpringSecurity + Vue的个人博客管理系统。
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
export default {
  name: 'Dashboard',
  computed: {
    ...mapGetters([
      'name'
    ])
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  &-container {
    margin: 30px;
  }
  &-text {
    font-size: 30px;
    line-height: 46px;
  }
}

</style>
