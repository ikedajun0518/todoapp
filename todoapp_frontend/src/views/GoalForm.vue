<template>
    <div>
        <form @submit.prevent="submit">
            <div class="goal-name-container">
                <input class="input__goal-name" v-model.trim="form.name" max-length="200" placeholder="タスクの目標・タイトルを記入">
                <p class="err" v-if="fieldError('name')">{{ fieldError('name') }}</p>
            </div>

            <div>
                <textarea v-model.trim="form.description" maxlength="1000" placeholder="説明があれば記入"></textarea>
                <p class="err" v-if="fieldError('description')">{{ fieldError('description') }}</p>
            </div>

            <div class="tasks-section">
                <div class="tasks-header">
                    <label>タスク（1件以上必須）</label>
                </div>
                <div v-for="(t, idx) in tasks" :key="t.key" class="task-row">
                    <input
                    v-model.trim="t.name"
                    maxlength="200"
                    placeholder="タスク名を記入"
                    class="task-input"
                    />
                    <button type="button" @click="confirmRemove(idx)" class="btn btn-danger btn-circle">ー</button>
                </div>
                <p class="err" v-if="taskListError">{{ taskListError }}</p>
            </div>

            <div class="task-add-button-container">
                <button type="button" class="btn btn-edit btn-circle" @click="addTask">＋</button>
            </div>

            <div class="actions">
                <button class="btn btn-submit" type="submit">{{ isEdit ? '更新' : '作成' }}</button>
                <router-link class="btn btn-back" to="/goals">キャンセル</router-link>
            </div>

            <p class="err" v-if="screenError">{{ screenError }}</p>
        </form>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue';
    import http from '@/api/http';

    interface GoalDetail {
        id: number;
        name: string;
        description?: string;
        tasks: {
            id: number;
            name: string;
            completed: boolean
        } [];
    };

    interface Task {
        id?: number;
        name: string;
        completed?: boolean;
    }

    interface FieldError {
        field: string | null;
        message: string;
    }

    export default Vue.extend({
        data() {
            return {
                isEdit: false,
                form: { name: '', description: '' } as { name: string, description?: string },
                tasks: [] as Array<{ key: string; id?: number; name: string }>,
                errors: [] as FieldError[],
                submitting: false,
            }
        },
        async created() {
            this.isEdit = this.$route.name === 'GoalEdit';
            if (this.isEdit) {
                const { data } = await http.get<GoalDetail>(`/goals/${this.$route.params.id}/detail`);
                this.form.name = data.name;
                this.form.description = data.description || '';
                this.tasks = (data.tasks || []).map(t => ({key: `ex-${t.id}`, id: t.id, name: t.name}));
                if (this.tasks.length === 0) this.tasks = [{ key: 'new-0', name: ''}];
            } else {
                this.tasks = [{ key: 'new-0', name: '' }];
            }
        },
        computed: {
            screenError(): string | null {
                const hit = this.errors.find(e => !e.field);
                return hit ? hit.message : null;
            },
            taskListError(): string | null {
                if (this.tasks.length < 1) return 'タスクは1件以上必要です'
                const bad = this.tasks.find(t => !t.name || t.name.trim().length ===0);
                if (bad) return '空白のみの入力不可'
                return null;
            }
        },
        methods: {
            fieldError(n: string): string {
                const hit = this.errors.find(e => e.field === n)
                return hit ? hit.message : '';
            },
            addTask() {
                const key = `new-${Date.now()}-${Math.random()}`
                this.tasks.push({ key, name: '' });
            },
            confirmRemove(idx: number) {
              if (this.tasks.length <= 1) {
                alert('タスクは1件以上必要です');
                return;
              } 
              if (confirm('このタスクを削除しますか？')) this.tasks.splice(idx, 1);
            },
            normalizeErrors(err: any) {
                this.errors = [];
                const body = err?.pretty || err?.response?.data;
                if (!body) return;
                if (Array.isArray(body.errors)) {
                    body.errors.forEach((e: { field: any; param: any; message: any; }) => {
                        const field = e.field || e.param || null;
                        this.errors.push({ field, message: e.message || body.message});
                    });
                } else if (body.message) {
                    this.errors.push({ field: null, message: body.message });
                }
            },
            async submit() {
                if (this.taskListError) {
                    this.errors = [{ field: null, message: this.taskListError }];
                    return;
                }
                const payload = {
                    name: this.form.name,
                    description: this.form.description,
                    tasks: this.tasks.map<Task>(t => ({ id: t.id, name: t.name.trim() }))
                }
                this.submitting = true;
                try {
                    if (this.isEdit) {
                        await http.put(`/goals/${this.$route.params.id}`, payload);
                    } else {
                        await http.post('/goals', payload);
                    }
                    this.$router.push({ name: 'GoalList' });
                } catch(e: any) {
                    this.normalizeErrors(e);
                } finally {
                    this.submitting = false;
                }
            }
        }
    })
</script>