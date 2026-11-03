<template>
    <div style="min-height: 400px">
        <BasicForm @register="registerForm">
            <template #workshopCode="{ model, field }">
                <WorkshopCodeInput
                    v-model:value="model[field]"
                    :disabled="formDisabled"
                    :isUpdate="isUpdate"
                />
            </template>
        </BasicForm>
        <div style="width: 100%;text-align: center" v-if="!formDisabled && showSubmitButton">
            <a-button @click="submitForm" pre-icon="ant-design:check" type="primary">提 交</a-button>
        </div>
    </div>
</template>

<script lang="ts">
    import {BasicForm, useForm} from '/@/components/Form/index';
    import {computed, defineComponent} from 'vue';
    import {defHttp} from '/@/utils/http/axios';
    import { propTypes } from '/@/utils/propTypes';
    import WorkshopCodeInput from './WorkshopCodeInput.vue';
    import {getBpmFormSchema} from '../MesWorkshop.data';
    import {saveOrUpdate} from '../MesWorkshop.api';

    export default defineComponent({
        name: "MesWorkshopForm",
        components:{
            BasicForm,
            WorkshopCodeInput
        },
        props:{
            formData: propTypes.object.def({}),
            formBpm: propTypes.bool.def(true),
            showSubmitButton: propTypes.bool.def(true),
        },
        setup(props){
            const [registerForm, { setFieldsValue, setProps,validate, scrollToField, getFieldsValue }] = useForm({
                labelWidth: 150,
                schemas: getBpmFormSchema(props.formData),
                showActionButtonGroup: false,
                baseColProps: {span: 24}
            });

            const formDisabled = computed(()=>{
                if(props.formData.disabled === false){
                    return false;
                }
                return true;
            });

            const isUpdate = computed(() => !!props.formData?.dataId || !!props.formData?.id);

            let formData = {};
            const queryByIdUrl = '/mes/mesWorkshop/queryById';
            async function initFormData(){
                let params = {id: props.formData.dataId};
                const data = await defHttp.get({url: queryByIdUrl, params});
                formData = {...data}
                if (formData.responsiblePersonId != null) {
                    formData.responsiblePersonId = String(formData.responsiblePersonId);
                }
                if (formData.isEnabled != null) {
                    formData.isEnabled = String(formData.isEnabled);
                }
                //设置表单的值
                await setFieldsValue(formData);
                //默认是禁用
                await setProps({disabled: formDisabled.value})
            }

            async function submitForm(showTip = true) {
                try {
                    let values = await validate();
                    console.log('表单数据', values)
                    if (Array.isArray(values.responsiblePersonId)) {
                        values.responsiblePersonId = values.responsiblePersonId.length > 0 ? values.responsiblePersonId[0] : null;
                    }
                    if (values.isEnabled != null) {
                        values.isEnabled = Number(values.isEnabled);
                    }
                    //提交表单
                    await saveOrUpdate(values, true,showTip);
                } catch ({ errorFields }) {
                   if (errorFields) {
                     const firstField = errorFields[0];
                     if (firstField) {
                       scrollToField(firstField.name, { behavior: 'smooth', block: 'center' });
                     }
                   }
                   return Promise.reject(errorFields);
                }
            }

            initFormData();
            
            return {
                registerForm,
                formDisabled,
                isUpdate,
                submitForm,
            }
        }
    });
</script>