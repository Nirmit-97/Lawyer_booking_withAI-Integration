// Auth utility functions for JWT token management

export const getActiveRole = () => {
  const role = sessionStorage.getItem('activeRole');
  // console.debug(`[AuthUtils] getActiveRole: ${role}`);
  return role;
};

export const setActiveRole = (role) => {
  console.log(`[AuthUtils] Setting activeRole to: ${role}`);
  if (role) {
    sessionStorage.setItem('activeRole', role);
  } else {
    sessionStorage.removeItem('activeRole');
  }
};

export const getToken = () => {
  const role = getActiveRole();
  let token = null;

  if (role === 'user') token = localStorage.getItem('userToken');
  else if (role === 'lawyer') token = localStorage.getItem('lawyerToken');
  else if (role === 'admin') token = localStorage.getItem('token');

  if (role && !token) {
    console.warn(`[AuthUtils] Role ${role} detected in sessionStorage but NO TOKEN found in localStorage!`);
  }

  return token;
};

export const login = (role, token, data) => {
  // ISOALTON: We no longer call removeToken() here.
  // This allows a User to be logged in Tab A and a Lawyer in Tab B simultaneously.

  setActiveRole(role);
  if (role === 'user') {
    localStorage.setItem('userToken', token);
    localStorage.setItem('userData', JSON.stringify(data));
  } else if (role === 'lawyer') {
    localStorage.setItem('lawyerToken', token);
    localStorage.setItem('lawyerData', JSON.stringify(data));
  } else if (role === 'admin') {
    localStorage.setItem('token', token);
    localStorage.setItem('adminData', JSON.stringify(data));
  }
};

export const removeToken = () => {
  const role = getActiveRole();

  // Granular removal: Only remove the data for the ACTIVE role in this tab
  if (role === 'user') {
    localStorage.removeItem('userToken');
    localStorage.removeItem('userData');
  } else if (role === 'lawyer') {
    localStorage.removeItem('lawyerToken');
    localStorage.removeItem('lawyerData');
  } else if (role === 'admin') {
    localStorage.removeItem('token');
    localStorage.removeItem('adminData');
  }

  sessionStorage.removeItem('activeRole');
};

export const getAuthHeaders = () => {
  const token = getToken();
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  };
};

export const isAuthenticated = () => {
  return !!getToken();
};

export const getUserType = () => {
  return getActiveRole();
};

export const getSessionData = () => {
  const role = getActiveRole();
  try {
    if (role === 'user') return JSON.parse(localStorage.getItem('userData') || '{}');
    if (role === 'lawyer') return JSON.parse(localStorage.getItem('lawyerData') || '{}');
    if (role === 'admin') return JSON.parse(localStorage.getItem('adminData') || '{}');
  } catch (e) {
    console.error('Error parsing session data', e);
  }
  return {};
};

export const getUserId = () => {
  const data = getSessionData();
  return data.id || null;
};

