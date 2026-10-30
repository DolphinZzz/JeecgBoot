import {
  defineAsyncComponent,
  // FunctionalComponent, CSSProperties
} from 'vue';
import { Spin } from 'ant-design-vue';
const noop = () => {};
// const Loading: FunctionalComponent<{ size: 'small' | 'default' | 'large' }> = (props) => {
//   const style: CSSProperties = {
//     position: 'absolute',
//     display: 'flex',
//     justifyContent: 'center',
//     alignItems: 'center',
//   };
//   return (
//     <div style={style}>
//       <Spin spinning={true} size={props.size} />
//     </div>
//   );
// };

interface Options {
  size?: 'default' | 'small' | 'large';
  delay?: number;
  timeout?: number;
  loading?: boolean;
  retry?: boolean;
}

export function createAsyncComponent(loader: Fn, options: Options = {}) {
  const { size = 'small', delay = 100, timeout = 30000, loading = false, retry = true } = options;
  return defineAsyncComponent({
    loader,
    loadingComponent: loading ? <Spin spinning={true} size={size} /> : undefined,
    // 加载失败时给出可见提示：没有 errorComponent 时 Vue 只渲染一个注释节点，
    // 表单里就变成「只有标签、没有输入框」，作业员和开发都看不出发生了什么。
    errorComponent: <span style={{ color: '#ff4d4f', fontSize: '12px' }}>控件加载失败，请刷新页面</span>,
    // The error component will be displayed if a timeout is
    // provided and exceeded. Default: Infinity.
    // TODO
    timeout,
    // Defining if component is suspensible. Default: true.
    // suspensible: false,
    delay,
    /**
     *
     * @param {*} error Error message object
     * @param {*} retry A function that indicating whether the async component should retry when the loader promise rejects
     * @param {*} fail  End of failure
     * @param {*} attempts Maximum allowed retries number
     */
    onError: !retry
      ? noop
      : (error, retry, fail, attempts) => {
          // Vite 重新预构建依赖、热更新期间动态 import 会拿到 504/HTML，不只是 fetch 报错，
          // 而这类失败是暂时的：只重试 fetch 错误会让控件从此留空，所以统一重试。
          console.warn(`[createAsyncComponent] 控件加载失败（第 ${attempts} 次）：${error?.message}`);
          if (attempts <= 3) {
            retry();
          } else {
            fail();
          }
        },
  });
}
