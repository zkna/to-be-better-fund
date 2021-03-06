import type { MockMethod } from 'vite-plugin-mock';
import { userModel, routeModel } from '../model';

const apis: MockMethod[] = [
  {
    url: '/mock/getUserRoutes',
    method: 'post',
    response: (options: Service.MockOption): Service.MockServiceResult => {
      const { userId = undefined } = options.body;

      const routeHomeName: AuthRoute.RouteKey = 'dashboard_analysis';

      const role = userModel.find(item => item.userId === userId)?.userRole || 'user';

      const filterRoutes = routeModel[role];

      return {
        code: "00000",
        msg: 'ok',
        data: {
          routes: filterRoutes,
          home: routeHomeName
        }
      };
    }
  }
];

export default apis;
