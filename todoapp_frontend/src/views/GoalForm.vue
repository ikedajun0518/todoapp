<template>
  <div class="form-container">
    <form @submit.prevent="submit">
      <div v-if="!deletionProtected" class="goal-name-container">
        <input
          class="input__goal-name"
          v-model.trim="form.name"
          maxlength="200"
          placeholder="タスクの目標・タイトルを記入"
        />
        <p class="err" v-if="fieldError('name')">{{ fieldError('name') }}</p>
      </div>
      <div v-else>
        <h2>{{ form.name }}</h2>
      </div>

      <div v-if="!deletionProtected">
        <textarea
          v-model.trim="form.description"
          maxlength="1000"
          placeholder="説明があれば記入"
        ></textarea>
        <p class="err" v-if="fieldError('description')">{{ fieldError('description') }}</p>
      </div>
      <div v-else>
        <div>{{ form.description }}</div>
      </div>

      <div class="tasks-section">
        <div class="tasks-header">
          <label>タスク（1件以上必須）</label>
        </div>

        <div v-for="(t, idx) in tasks" :key="t.key" class="task-row">
          <div class="task-input-container">
            <input
              v-model.trim="t.name"
              maxlength="200"
              placeholder="タスク名を記入"
              class="task-input"
              :aria-invalid="!!taskError(idx)"
            />
            <button
              type="button"
              @click="confirmRemove(idx)"
              class="btn btn-danger btn-circle"
            >ー</button>
          </div>
          <p class="err" v-if="taskError(idx)">{{ taskError(idx) }}</p>
        </div>
      </div>

      <div class="task-add-button-container">
        <button type="button" class="btn btn-edit btn-circle" @click="addTask">＋</button>
      </div>

      <div class="actions">
        <button class="btn btn-submit" type="submit">{{ isEdit ? '更新' : '作成' }}</button>
        <router-link v-if="!isEdit" class="btn btn-back" to="/goals">キャンセル</router-link>
        <router-link v-else class="btn btn-back" :to="'/goals/' + $route.params.id">キャンセル</router-link>
      </div>
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
  deletionProtected?: boolean;
  tasks: { id: number; name: string; completed: boolean }[];
}

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
      form: { name: '', description: '' } as { name: string; description?: string },
      tasks: [] as Array<{ key: string; id?: number; name: string }>,
      errors: [] as FieldError[],
      submitting: false,
      deletionProtected: false,
      taskErrors: {} as Record<string, string>,
    };
  },
  async created() {
    this.isEdit = this.$route.name === 'GoalEdit';
    if (this.isEdit) {
      const { data } = await http.get<GoalDetail>(`/goals/${this.$route.params.id}/detail`);
      this.form.name = data.name;
      this.form.description = data.description || '';
      this.deletionProtected = data.deletionProtected || false;
      this.tasks = (data.tasks || []).map(t => ({ key: `ex-${t.id}`, id: t.id, name: t.name }));
      if (this.tasks.length === 0) this.tasks = [{ key: 'new-0', name: '' }];
    } else {
      this.tasks = [{ key: 'new-0', name: '' }];
    }
  },
  methods: {
    fieldError(n: string): string {
      const hit = this.errors.find(e => e.field === n);
      return hit ? hit.message : '';
    },
    addTask() {
      const key = `new-${Date.now()}-${Math.random()}`;
      this.tasks.push({ key, name: '' });
    },
    confirmRemove(idx: number) {
      if (this.tasks.length <= 1) {
        alert('タスクは1件以上必要です');
        return;
      }
      if (confirm('このタスクを削除しますか？')) this.tasks.splice(idx, 1);
    },
    localTaskError(name: string): string | null {
      const v = (name || '').trim();
      if (!v) return 'タスク名は必須です';
      if (v.length > 200) return '200文字以内で入力してください';
      return null;
    },
    taskError(idx: number): string | null {
      const t = this.tasks[idx];
      if (!t) return null;
      const serverMsg = this.taskErrors[t.key];
      if (serverMsg) return serverMsg;
      return this.localTaskError(t.name);
    },
    normalizeErrors(err: any) {
      this.errors = [];
      this.taskErrors = {};
      const body = err?.pretty || err?.response?.data;
      if (!body) return;

      if (Array.isArray(body.errors)) {
        body.errors.forEach((e: { field?: string; param?: string; message?: string }) => {
          const field = e.field || e.param || '';
          const message = e.message || body.message || 'Validation error';

          const m = field.match(/^tasks\[(\d+)\]\.name$/);
          if (m) {
            const idx = Number(m[1]);
            const key = this.tasks[idx]?.key;
            if (key) this.taskErrors[key] = message;
          } else if (field) {
            this.errors.push({ field, message });
          } else {
            this.errors.push({ field: null, message });
          }
        });
      } else if (body.message) {
        this.errors.push({ field: null, message: body.message });
      }
    },
    async submit() {
      this.errors = [];
      this.taskErrors = {};

      if (this.tasks.length < 1) {
        this.errors = [{ field: null, message: 'タスクは1件以上必要です' }];
        return;
      }
      let hasAnyError = false;
      this.tasks.forEach(t => {
        const msg = this.localTaskError(t.name);
        if (msg) {
          this.taskErrors[t.key] = msg;
          hasAnyError = true;
        }
      });
      if (hasAnyError) return;

      const payload = {
        name: this.form.name,
        description: this.form.description,
        tasks: this.tasks.map<Task>(t => ({ id: t.id, name: (t.name || '').trim() })),
      };

      this.submitting = true;
      try {
        if (this.isEdit) {
          await http.put(`/goals/${this.$route.params.id}`, payload);
        } else {
          await http.post('/goals', payload);
        }
        this.$router.push({ name: 'GoalList' });
      } catch (e: any) {
        this.normalizeErrors(e);
      } finally {
        this.submitting = false;
      }
    },
  },
});
</script>