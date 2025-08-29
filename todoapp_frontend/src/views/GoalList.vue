<template>
  <div>
    <div class="top-container">
      <div class="search-box">
        <input
          class="search-box__input"
          type="text"
          v-model.trim="q"
          placeholder="目標名・タスク名で検索"
          @input="onInput"
        />
      </div>
      <h1>目標一覧</h1>
      <div class="toolbar">
        <router-link class="btn btn-add-goal" :to="{ name: 'GoalCreate' }">新規目標</router-link>
      </div>
    </div>

    <!-- 検索中（qあり）：検索結果 -->
    <div v-if="showingSearch" class="search-results table--scroll">
      <table class="list list--clickable">
        <tbody>
          <tr
            v-for="g in searchResults"
            :key="'s-' + g.id"
            class="list__row"
            @click="toDetail(g.id)"
          >
            <td class="list__cell--name">
              <span v-html="g.nameHtml"></span>
              <!-- タスクの部分一致（あれば下部に表示） -->
              <ul v-if="g.tasks && g.tasks.length" class="task-hits">
                <li v-for="t in g.tasks" :key="t.id" class="task-hits__item">
                  <span class="task-hits__bullet">・</span>
                  <span v-html="t.nameHtml"></span>
                </li>
              </ul>
            </td>
            <td class="list__updatedAt">{{ toDate(g.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 通常一覧（q空）：全件 -->
    <table v-else class="list list--clickable table--scroll">
      <tbody>
        <tr v-for="g in items" :key="g.id" class="list__row" @click="toDetail(g.id)">
          <td class="list__cell--name">{{ g.name }}</td>
          <td class="list__updatedAt">{{ toDate(g.updatedAt) }}</td>
        </tr>
      </tbody>
    </table>

    <p class="err" v-if="error">{{ error }}</p>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import http from '@/api/http';

interface Goal {
  id: number;
  name: string;
  description?: string;
  deletionProtected: boolean;
  createdAt: string;
  updatedAt: string;
}

interface TaskHitDto {
  id: number;
  nameHtml: string;
  updatedAt: string;
}
interface SearchResultDto {
  id: number;
  nameHtml: string;
  updatedAt: string;
  tasks: TaskHitDto[];
}

export default Vue.extend({
  data() {
    return {
      q: '' as string,
      debounceId: 0 as any,
      items: [] as Goal[],
      searchResults: [] as SearchResultDto[],
      loading: false as boolean,
      error: null as string | null
    };
  },
  computed: {
    showingSearch(): boolean {
      return this.q.trim().length > 0;
    }
  },
  created() {
    this.loadAll();
  },
  methods: {
    toDetail(id: number) {
      this.$router.push({ name: 'GoalDetail', params: { id: String(id) } });
    },
    toDate(v: string | Date) {
      const d = (typeof v === 'string') ? new Date(v) : v;
      return isNaN(d.getTime()) ? '' : d.toLocaleString();
    },
    async loadAll() {
      this.loading = true;
      this.error = null;
      try {
        const res = await http.get('/goals', { params: { page: 0, size: 1000 } });
        this.items = (res.data && res.data.content) ? res.data.content : res.data;
      } catch (e: any) {
        this.error = e?.pretty?.message || e.message;
      } finally {
        this.loading = false;
      }
    },
    async searchNow() {
      const q = this.q.trim();
      if (!q) { this.searchResults = []; return; }
      this.loading = true;
      this.error = null;
      try {
        const res = await http.get<SearchResultDto[]>('/search/goals', { params: { q } });
        this.searchResults = res.data || [];
      } catch (e: any) {
        this.error = e?.pretty?.message || e.message;
      } finally {
        this.loading = false;
      }
    },
    onInput() {
      clearTimeout(this.debounceId);
      // 300ms デバウンス
      this.debounceId = setTimeout(() => {
        if (this.showingSearch) this.searchNow();
        else this.searchResults = [];
      }, 300);
    }
  }
});
</script>
