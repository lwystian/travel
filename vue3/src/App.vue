<template>

  <router-view/>
</template>

<style lang="scss">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

.el-overlay-dialog {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.el-overlay-dialog > .el-dialog {
  max-height: calc(100vh - 48px);
  margin: 0 !important;
  display: flex;
  flex-direction: column;
}

.el-overlay-dialog > .el-dialog .el-dialog__body {
  overflow: auto;
}

@media (max-width: 640px) {
  .el-overlay-dialog {
    padding: 12px;
  }

  .el-overlay-dialog > .el-dialog {
    width: min(100%, calc(100vw - 24px)) !important;
    max-height: calc(100vh - 24px);
  }
}

</style>
