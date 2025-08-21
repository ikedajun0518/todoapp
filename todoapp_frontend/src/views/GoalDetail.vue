<template>
    <div class="goal-detail">
        <div class="goal-detail__head">
            <h2 class="goal-detail__title">{{ goal?.name }}</h2>
            <router-link class="btn" :to="{ name: 'GoalEdit', params: {id: $route.params.id} }">編集</router-link>
        </div>

        <section class="goal-detail__section">
            <h3>説明</h3>
            <p class="goal-detail__desc">{{ goal?.description || '(なし)' }}</p>
        </section>

        <section class="goal-detail__section">
            <h3>タスク</h3>
            <ul class="task-list">
                <li v-for="t in tasks" :key="t.id" class="task-list__item">
                    <label class="task-list__row">
                        <input type="checkbox" :checked="t.completed" @change="toggle(t)" />
                        <span :class="['task-list__label', { 'is-done': t.completed }]">{{ t.name }}</span>
                    </label>
                </li>
            </ul>
            <p class="err" v-if="error">{{ error }}</p>
        </section>

        <section class="goal-detail__section meta">
            <div>作成: {{ dt(goal?.createdAt) }}</div>
            <div>更新: {{ dt(goal?.updatedAt) }}</div>
        </section>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue';
    import http from '@/api/http';

    interface Task {
        id: number;
        name: string;
        completed: boolean;
        createdAt: string;
        updatedAt: string;
    }

    interface GoalDetail {
        id: number;
        name: string;
        description?: string;
        createdAt: string;
        updatedAt: string;
        tasks: Task[];
    }

    export default Vue.extend({
        data() {
            return {
                goal: null as GoalDetail | null,
                tasks: [] as Task[],
                error: null as string | null,
                loading: false
            }
        },
        async created() {
            await this.load();
        },
        methods: {
            async load() {
                this.loading = true;
                this.error = null;
                try {
                    const { data } = await http.get<GoalDetail>(`/goals/${this.$route.params.id}/detail`);
                    this.goal = data;
                    this.tasks = data.tasks || [];
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message;
                } finally {
                    this.loading = false;
                }
            },
            async toggle(t: Task) {
                const next = !t.completed;
                try {
                    await http.patch(`/tasks/${t.id}/completed`, { completed: next });
                    t.completed = next;
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message;
                }
            },
            dt(v?: string) {
                return v ? new Date(v).toLocaleString() : '';
            }
        }
    })
</script>