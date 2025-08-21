<template>
    <div>
        <div class="toolbar">
            <router-link class="btn" :to="{ name: 'GoalCreate' }">＋　新規目標</router-link>

            <label>並び替え:
                <select v-model="sortKey" @change="load">
                    <option value="updatedAt,desc">更新日 ↓</option>
                    <option value="goal,asc">名前 ↑</option>
                </select>
            </label>
        </div>

        <table class="list list--clickable">
            <thead>
                <tr><th>目標</th><th>更新日</th></tr>
            </thead>
            <tbody>
                <tr v-for="g in page.content" :key="g.id" class="list__row" @click="toDetail(g.id)">
                    <td class="list__cell--name">{{ g.name }}</td>
                    <td>{{ new Date(g.updatedAt).toLocaleString() }}</td>
                </tr>
            </tbody>
        </table>
        <div class="pager" v-if="page.totalPages > 1">
            <button :disabled="pageable.page===0" @click="go(pageable.page-1)">前</button>
            <span>{{ pageable.page+1 }} / {{ page.totalPages }}</span>
            <button :disabled="pageable.page>=page.totalPages-1" @click="go(pageable.page+1)">次</button>
        </div>

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
    interface Page<T> {
        content: T[];
        totalPages: number;
        totalElements: number;
        number: number;
        size: number;
    }

    export default Vue.extend({
        data() {
            return {
                page: {
                    content:[],
                    totalPages: 0,
                    totalElements: 0,
                    number: 0,
                    size: 10
                } as Page<Goal>,
                pageable: { page: 0, size: 10 },
                sortKey: 'updatedAt,desc',
                loading: false as boolean,
                error: null as string | null
            }
        },
        created() {
            const q = this.$route.query;
            this.pageable.page = +(q.page || 0);
            this.pageable.size = +(q.size || 10);
            this.sortKey = (q.sort as string) || 'updatedAt,desc';
            this.load();
        },
        methods: {
            toDetail(id: number) {
                this.$router.push({ name: 'GoalDetail', params: { id: String(id) } });
            },
            async load() {
                this.loading = true;
                this.error = null;
                try {
                    const res = await http.get('/goals', {
                        params: { page: this.pageable.page, size: String(this.pageable.size), sort: this.sortKey }
                    });
                    this.page = res.data;
                    this.$router.replace({ query: {
                        page: String(this.pageable.page), size: String(this.pageable.size), sort: this.sortKey
                    }});
                } catch (e: any) {
                    this.error = e?.pretty?.message || e.message;
                } finally {
                    this.loading = false;
                }
            },
            go (p: number) {
                this.pageable.page = p;
                this.load();
            }
        }
    })
</script>