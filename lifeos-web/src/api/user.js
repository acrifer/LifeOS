import request from './request'

// User operations
export const userApi = {
    // Login
    login(data) {
        return request({
            url: '/user/login',
            method: 'post',
            data
        })
    },

    // Register
    register(data) {
        return request({
            url: '/user/register',
            method: 'post',
            data
        })
    },

    // Get Info
    getInfo() {
        return request({
            url: '/user/info',
            method: 'get'
        })
    }
}
