import Vue from "vue"
import Router from 'vue-router'
import GoalList from '@/views/GoalList.vue'
import GoalForm from '@/views/GoalForm.vue'
import GoalDetail from "@/views/GoalDetail.vue"

Vue.use(Router);

export default new Router({
    mode: 'history',
    routes: [
        { path: '/', redirect: '/goals' },
        { path: '/goals', component: GoalList, name: 'GoalList' },
        { path: '/goals/:id', component: GoalDetail, name: 'GoalDetail', props: true },
        { path: '/goals/new', component: GoalForm, name: 'GoalCreate' },
        { path: '/goals/:id/edit', component: GoalForm, name: 'GoalEdit', props: true }
    ]
});