<template>
    <div class="goal-detail">
        <div class="goal-detail__head">
            <h2 class="goal-detail__title">{{ goal?.name }}</h2>
            <div class="goal-detail__actions">
                <router-link class="btn btn-edit" :to="{ name: 'GoalEdit', params: {id: $route.params.id} }">編集</router-link>
                <button 
                    class="btn btn-danger"
                    :disabled="goal?.deletionProtected"
                    :title="goal?.deletionProtected ? '削除不可の項目です' : ''"
                    @click="onDelete">削除</button>
            </div>
        </div>

        <section class="goal-detail__section">
            <p class="goal-detail__desc">{{ goal?.description || '(説明なし)' }}</p>
        </section>

        <section class="goal-detail__section">
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

        <div class="goal-detail__footer">
            <button class="btn btn-back" @click="onBack">一覧へ戻る</button>
        </div>
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
        deletionProtected?: boolean;
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
                    await this.load();
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message;
                }
            },
            async onDelete() {
                if (!this.goal) return;
                const ok = confirm('この目標を削除します。\n配下のタスクもすべて削除されます。よろしいですか？');
                if (!ok) return;
                try {
                    await http.delete(`/goals/${this.goal.id}`);
                    this.$router.push({ name: 'GoalList' });
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message || '削除に失敗しました';
                }
            },
            dt(v?: string) {
                return v ? new Date(v).toLocaleString() : '';
            },
            onBack() {
                this.$router.push({ name: 'GoalList' });
            }
        }
    })
</script>