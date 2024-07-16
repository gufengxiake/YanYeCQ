package nckd.yanye.scm.plugin.operate;

import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.*;

/**
 * 演示操作插件全部事件的捕获及触发时机
 *
 * @author rd_johnnyding
 * @remark 本示例代码，捕捉的事件，由前往后触发
 */
public class OperationEventSample extends AbstractOperationServicePlugIn {

    /**
     * 操作执行，加载单据数据包之前，触发此事件；
     *
     * @remark 在单据列表上执行单据操作，传入的是单据内码；
     * 系统需要先根据传入的单据内码，加载单据数据包，其中只包含操作要用到的字段，然后再执行操作；
     * 在加载单据数据包之前，操作引擎触发此事件；
     * <p>
     * 插件需要在此事件，添加需要用到的字段；
     * 否则，系统加载的单据数据包，可能没有插件要用到的字段值，从而引发中断
     */
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        this.printEventInfo("", "");
    }

    /**
     * 构建好操作校验器之后，执行校验之前，触发此事件；
     *
     * @remark 插件可以在此事件，增加自定义操作校验器，或者去掉内置的校验器
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        this.printEventInfo("", "");
    }

    /**
     * 操作校验通过之后，开启事务之前，触发此事件；
     *
     * @remark 插件可以在此事件，对通过校验的数据，进行整理
     */
    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        this.printEventInfo("", "");
    }

    /**
     * 操作校验通过，开启了事务，准备把数据提交到数据库之前触发此事件；
     *
     * @remark 可以在此事件，进行数据同步处理
     */
    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        this.printEventInfo("", "");
    }

    /**
     * 单据数据已经提交到数据库之后，事务未提交之前，触发此事件；
     *
     * @remark 可以在此事件，进行数据同步处理；
     */
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        this.printEventInfo("", "");

    }

    /**
     * 操作事务提交失败，事务回滚之后触发此事件；
     *
     * @remark 该方法在事务异常后执行，插件可以在此事件，对没有事务保护的数据更新进行补偿
     */
    @Override
    public void rollbackOperation(RollbackOperationArgs e) {
        this.printEventInfo("", "");
    }

    /**
     * 操作执行完毕，事务提交之后，触发此事件；
     *
     * @remark 插件可以在此事件，处理操作后续事情，与操作事务无关
     */
    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        this.printEventInfo("", "");
    }

    private void printEventInfo(String eventName, String argString) {
        String msg = String.format("%s : %s", eventName, argString);
        System.out.println(msg);
    }
}