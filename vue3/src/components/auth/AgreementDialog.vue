<template>
  <el-dialog v-model="visible" title="用户服务协议与隐私保护条款" width="760px" class="agreement-dialog">
    <div class="agreement-content">
      <section>
        <h3>一、服务说明</h3>
        <p>本平台为用户提供景点浏览、攻略发布、评论互动、行程预订、订单支付、收藏管理、站内消息等旅行相关服务。用户注册、登录及使用平台功能时，应遵守法律法规、平台规则和公序良俗。</p>
      </section>
      <section>
        <h3>二、账号与安全</h3>
        <p>用户应使用本人合法持有的手机号注册账号，并妥善保管密码、短信验证码和登录状态。验证码仅用于本人认证，不得转让、出售或泄露给他人。因用户主动泄露或保管不善造成的损失，由用户自行承担。</p>
      </section>
      <section>
        <h3>三、内容发布规范</h3>
        <p>用户发布攻略、评论、图片、订单备注等互动内容时，不得包含违法违规、暴力恐怖、淫秽色情、赌博诈骗、侵权盗版、恶意营销、侵犯隐私、危害公共安全或扰乱平台秩序的信息。平台将通过敏感词过滤、风控审计和人工审核进行管理。</p>
      </section>
      <section>
        <h3>四、个人信息处理</h3>
        <p>平台会基于注册登录、实名认证前置校验、订单履约、客服通知、安全审计和风险控制等必要目的处理手机号、登录设备、IP地址、操作日志、订单信息、评论内容和站内消息状态。平台将采取访问控制、传输加密、日志审计、最小化授权等措施保护信息安全。</p>
      </section>
      <section>
        <h3>五、短信与通知</h3>
        <p>用户同意平台向注册手机号发送验证码、安全提醒、订单状态、审核结果等必要通知。营销类消息如后续启用，应提供明确退订方式。平台不会向用户索要短信验证码、密码或支付凭证。</p>
      </section>
      <section>
        <h3>六、交易与订单</h3>
        <p>用户预订行程、酒店或相关服务时，应确认联系人、出行日期、数量和支付金额。订单取消、退款、改签、不可抗力处理等规则以页面展示、商家政策及法律规定为准。</p>
      </section>
      <section>
        <h3>七、平台管理</h3>
        <p>平台可对违反规则的内容进行拦截、隐藏、删除、降权或提交人工复核；对异常账号采取限制登录、限制发布、冻结或注销等措施。必要时，平台将依法配合主管部门进行安全管理和备案要求。</p>
      </section>
      <section>
        <h3>八、协议更新</h3>
        <p>平台可能根据业务变化、法律法规或安全要求更新本协议。重大变更将通过页面提示或站内消息通知用户。用户继续使用服务即视为接受更新后的协议。</p>
      </section>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="accept">我已阅读并同意</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'accept'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const accept = () => {
  emit('accept')
  visible.value = false
}
</script>

<style scoped>
.agreement-content {
  max-height: 58vh;
  overflow-y: auto;
  padding-right: 10px;
  color: #475569;
  line-height: 1.75;
}

.agreement-content section {
  margin-bottom: 18px;
}

.agreement-content h3 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 16px;
}

.agreement-content p {
  margin: 0;
  font-size: 14px;
}
</style>
