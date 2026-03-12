import request from './request'

export const noteApi = {
    // Create note
    create(data) {
        return request({
            url: '/note',
            method: 'post',
            data
        })
    },

    // Update note
    update(data) {
        return request({
            url: '/note',
            method: 'put',
            data
        })
    },

    // Delete note
    delete(noteId) {
        return request({
            url: `/note/${noteId}`,
            method: 'delete'
        })
    },

    // Get note detail
    getDetail(noteId) {
        return request({
            url: `/note/${noteId}`,
            method: 'get'
        })
    },

    // Get all user notes
    getList() {
        return request({
            url: '/note/list',
            method: 'get'
        })
    },

    // Search notes
    search(keyword) {
        return request({
            url: '/note/search',
            method: 'get',
            params: { keyword }
        })
    },

    // Generate or regenerate AI summary
    generateSummary(noteId) {
        return request({
            url: `/note/${noteId}/summary`,
            method: 'post',
            timeout: 15000
        })
    }
}
